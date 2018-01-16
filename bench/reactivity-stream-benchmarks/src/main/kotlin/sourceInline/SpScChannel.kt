package sourceInline

import internal.LockFreeSPSCQueue
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.suspendCancellableCoroutine

public open class SpScSendChannel<E : Any>(
        /**
         * Buffer capacity.
         */
        capacity: Int
) : Sink<E> {

    /** @suppress **This is unstable API and it is subject to change.** */
    protected val queue = LockFreeSPSCQueue(capacity)

    /**
     * Tries to add element to buffer or to queued receiver.
     * Return type is `OFFER_SUCCESS | OFFER_FAILED | SpScClosed`.
     * @suppress **This is unstable API and it is subject to change.**
     */
    protected fun offerInternal(element: E): Any {
        val result = queue.offer(element)
        return when {
            result == null -> OFFER_SUCCESS // OK element is in the buffer
            result is SpScClosed<*> -> result // Closed for send
            else -> OFFER_FAILED // buffer is full
        }
    }

    public final override suspend fun send(item: E) {
        // fast path -- try offer non-blocking
        if (offer(item)) return
        // slow-path does suspend
        return sendSuspend(item)
    }

    public final fun offer(element: E): Boolean {
        val result = offerInternal(element)
        return when {
            result === OFFER_SUCCESS -> true
            result === OFFER_FAILED -> false
            result is SpScClosed<*> -> throw result.sendException
            else -> error("offerInternal returned $result")
        }
    }

    private suspend fun sendSuspend(element: E): Unit = suspendCancellableCoroutine(holdCancellability = true) sc@ { cont ->
        val send = SpScSendElement(queue.nextValueToConsume(), element, cont)
        loop@ while (true) {
            val enqueueResult = enqueueSend(send)
            when (enqueueResult) {
                null -> { // enqueued successfully
                    cont.initCancellability() // make it properly cancellable
//                    cont.removeOnCancel(send)
                    return@sc
                }
                is SpScClosed<*> -> {
                    cont.resumeWithException(enqueueResult.sendException)
                    return@sc
                }
            }
            // hm... receiver is waiting or buffer is not full. try to offer
            val offerResult = offerInternal(element)
            when {
                offerResult === OFFER_SUCCESS -> {
                    cont.resume(Unit)
                    return@sc
                }
                offerResult === OFFER_FAILED -> continue@loop
                offerResult is SpScClosed<*> -> {
                    cont.resumeWithException(offerResult.sendException)
                    return@sc
                }
                else -> error("offerInternal returned $offerResult")
            }
        }
    }

    /**
     * Result is:
     * * null -- successfully enqueued
     * * ENQUEUE_FAILED -- buffer is not full (should not enqueue)
     * * SpScClosed<*> -- receiver is closed (should not enqueue)
     */
    private fun enqueueSend(send: SpScSendElement<E>): Any? {
        queue.modifyNextValueToConsumeIfPrev(send, send.value, { prev ->
            when (prev) {
                is SpScClosed<*> -> return@enqueueSend prev
                null -> return ENQUEUE_FAILED
            }
            true
        })
        return null
    }

    public override fun close(cause: Throwable?) {
        val closed = SpScClosed<E>(cause)
        queue.close(closed)
    }
}

class SpScChannel<E : Any>(capacity: Int) : SpScSendChannel<E>(capacity) {

    /**
     * Tries to remove element from buffer
     * Return type is `E | POLL_FAILED | Closed`
     * @suppress **This is unstable API and it is subject to change.**
     */
    protected fun pollInternal(): Any? {
        val value = queue.poll()
        return when {
            value is SpScClosed<*> -> value
            value is SpScSend -> {
                val token = value.tryResumeSend(idempotent = null)
                if (token != null) {
                    value.completeResumeSend(token)
                    value.pollResult
                } else {
                    POLL_FAILED
                }
            }
            value == null -> POLL_FAILED// buffer is Empty
            else -> value
        }
    }

    @Suppress("UNCHECKED_CAST")
    public final suspend fun receive(): E {
        // fast path -- try poll non-blocking
        val result = pollInternal()
        if (result !== POLL_FAILED) return receiveResult(result)
        // slow-path does suspend
        return receiveSuspend()
    }

    @Suppress("UNCHECKED_CAST")
    private fun receiveResult(result: Any?): E {
        if (result is Closed<*>) throw result.receiveException
        return result as E
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun receiveSuspend(): E = suspendCancellableCoroutine(holdCancellability = true) sc@ { cont ->
        val receive = ReceiveElement(cont as CancellableContinuation<E?>, nullOnClose = false)
        while (true) {
            if (enqueueReceive(receive)) {
                cont.initCancellability() // make it properly cancellable
                removeReceiveOnCancel(cont, receive)
                return@sc
            }
            // hm... something is not right. try to poll
            val result = pollInternal()
            if (result is Closed<*>) {
                cont.resumeWithException(result.receiveException)
                return@sc
            }
            if (result !== POLL_FAILED) {
                cont.resume(result as E)
                return@sc
            }
        }
    }

    private fun enqueueReceive(receive: Receive<E>): Boolean {
        val result = if (isBufferAlwaysEmpty)
            queue.addLastIfPrev(receive, { it !is Send }) else
            queue.addLastIfPrevAndIf(receive, { it !is Send }, { isBufferEmpty })
        if (result) onEnqueuedReceive()
        return result
    }

    public final fun iterator(): ChannelIterator<E> = Itr(this)

    private class Itr<E: Any>(val channel: SpScChannel<E>) : ChannelIterator<E> {
        var result: Any? = POLL_FAILED // E | POLL_FAILED | Closed

        override suspend fun hasNext(): Boolean {
            // check for repeated hasNext
            if (result !== POLL_FAILED) return hasNextResult(result)
            // fast path -- try poll non-blocking
            result = channel.pollInternal()
            if (result !== POLL_FAILED) return hasNextResult(result)
            // slow-path does suspend
            return hasNextSuspend()
        }

        private fun hasNextResult(result: Any?): Boolean {
            if (result is Closed<*>) {
                if (result.closeCause != null) throw result.receiveException
                return false
            }
            return true
        }

        private suspend fun hasNextSuspend(): Boolean = suspendCancellableCoroutine(holdCancellability = true) sc@ { cont ->
            val receive = ReceiveHasNext(this, cont)
            while (true) {
                if (channel.enqueueReceive(receive)) {
                    cont.initCancellability() // make it properly cancellable
                    channel.removeReceiveOnCancel(cont, receive)
                    return@sc
                }
                // hm... something is not right. try to poll
                val result = channel.pollInternal()
                this.result = result
                if (result is Closed<*>) {
                    if (result.closeCause == null)
                        cont.resume(false)
                    else
                        cont.resumeWithException(result.receiveException)
                    return@sc
                }
                if (result !== POLL_FAILED) {
                    cont.resume(true)
                    return@sc
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        override suspend fun next(): E {
            val result = this.result
            if (result is Closed<*>) throw result.receiveException
            if (result !== POLL_FAILED) {
                this.result = POLL_FAILED
                return result as E
            }
            // rare case when hasNext was not invoked yet -- just delegate to receive (leave state as is)
            return channel.receive()
        }
    }
}

internal const val DEFAULT_CLOSE_MESSAGE = "SpScChannel was closed"

/**
 * Represents sending waiter in the queue.
 * @suppress **This is unstable API and it is subject to change.**
 */
public interface SpScSend {
    val pollResult: Any? // E | Closed
    fun tryResumeSend(idempotent: Any?): Any?
    fun completeResumeSend(token: Any)
    fun resumeSendClosed(closed: SpScClosed<*>)
}

/**
 * Represents sender for a specific element.
 * @suppress **This is unstable API and it is subject to change.**
 */
@Suppress("UNCHECKED_CAST")
public class SpScSendElement<E : Any?>(
        override val pollResult: Any?,
        val value: E,
        @JvmField val cont: CancellableContinuation<Unit>
) : SpScSend {
    override fun tryResumeSend(idempotent: Any?): Any? = cont.tryResume(Unit, idempotent)
    override fun completeResumeSend(token: Any) = cont.completeResume(token)
    override fun resumeSendClosed(closed: SpScClosed<*>) = cont.resumeWithException(closed.sendException)
    override fun toString() = "SendElement($pollResult)[$cont]"
}

/**
 * Represents closed channel.
 * @suppress **This is unstable API and it is subject to change.**
 */
public class SpScClosed<in E>(
        @JvmField val closeCause: Throwable?
) : SpScSend, ReceiveOrClosed<E> {
    val sendException: Throwable get() = closeCause ?: ClosedSendChannelException(DEFAULT_CLOSE_MESSAGE)
    val receiveException: Throwable get() = closeCause ?: ClosedReceiveChannelException(DEFAULT_CLOSE_MESSAGE)

    override val offerResult get() = this
    override val pollResult get() = this
    override fun tryResumeSend(idempotent: Any?): Any? = CLOSE_RESUMED
    override fun completeResumeSend(token: Any) {
        check(token === CLOSE_RESUMED)
    }

    override fun tryResumeReceive(value: E, idempotent: Any?): Any? = CLOSE_RESUMED
    override fun completeResumeReceive(token: Any) {
        check(token === CLOSE_RESUMED)
    }

    override fun resumeSendClosed(closed: SpScClosed<*>) = error("Should be never invoked")
    override fun toString() = "Closed[$closeCause]"
}

private abstract class Receive<in E> : ReceiveOrClosed<E> {
    override val offerResult get() = OFFER_SUCCESS
    abstract fun resumeReceiveClosed(closed: SpScClosed<*>)
}

private class ReceiveElement<in E>(
        @JvmField val cont: CancellableContinuation<E?>,
        @JvmField val nullOnClose: Boolean
) : Receive<E>() {
    override fun tryResumeReceive(value: E, idempotent: Any?): Any? = cont.tryResume(value, idempotent)
    override fun completeResumeReceive(token: Any) = cont.completeResume(token)
    override fun resumeReceiveClosed(closed: SpScClosed<*>) {
        if (closed.closeCause == null && nullOnClose)
            cont.resume(null)
        else
            cont.resumeWithException(closed.receiveException)
    }
    override fun toString(): String = "ReceiveElement[$cont,nullOnClose=$nullOnClose]"
}
