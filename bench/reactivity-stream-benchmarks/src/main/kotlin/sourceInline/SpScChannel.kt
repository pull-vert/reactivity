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
        val send = FullElement(element, cont)
        loop@ while (true) {
            if (enqueueSend(send)) { // enqueued successfully
                cont.initCancellability() // make it properly cancellable
//                cont.removeOnCancel(send)
                return@sc
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
     * * true -- successfully enqueued
     * * false -- buffer is not full (should not enqueue)
     */
    private fun enqueueSend(send: FullElement<E>): Boolean {
        send.currentValue = queue.nextValueToConsume() ?: return false // if next value is null there is room to Send to
        return queue.modifyNextValueToConsumeIfPrev(send, send.currentValue, { prev ->
            // if prev value is null, there is room to Send to
            if (null == prev) return@enqueueSend false
            // otherwise buffer is still full, predicate is true
            true
        })
    }

    public override fun close(cause: Throwable?) {
        val closed = SpScClosed<E>(cause)
        queue.close(closed)
    }
}

class SpScChannel<E : Any>(capacity: Int) : SpScSendChannel<E>(capacity) {

    /**
     * Tries to remove element from buffer
     * Return type is `E | POLL_FAILED`
     * @suppress **This is unstable API and it is subject to change.**
     */
    protected fun pollInternal(): Any {
        val value = queue.poll()
        return when {
            value is FullElement<*> -> {
                queue.offer(value.offerValue) // we are sure there is at least one slot for this value
                value.resumeSend() // resume Send
                value.currentValue
            }
            value == null -> POLL_FAILED // buffer is Empty, or Closed
            else -> value
        }
    }

    @Suppress("UNCHECKED_CAST")
    public final suspend fun receive(): E {
        // fast path -- try poll non-blocking
        val result = pollInternal()
        if (result !== POLL_FAILED) return result as E
        val closed = queue.getClosed() as? SpScClosed<E> ?: return receiveSuspend() // slow-path does suspend
        throw closed.receiveException
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

    public final fun iterator(): ChannelIterator<E> = Itr(this)

    private class Itr<E : Any>(val channel: SpScChannel<E>) : ChannelIterator<E> {
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
 * Full element to store when suspend Producer
 */
private class FullElement<E : Any>(
        val offerValue: E,
        @JvmField val cont: CancellableContinuation<Unit>
) {
    lateinit var currentValue: Any
    fun resumeSend() = cont.resume(Unit)
    override fun toString() = "FullElement($offerValue)[$cont]"
}

private interface Empty<in E : Any> : ReceiveOrClosed<E> {
    abstract fun resumeReceiveClosed(closed: SpScClosed<*>)
}

private class EmptyElement(
        @JvmField val cont: CancellableContinuation<Unit>
) {
    fun resumeReceive() = cont.resume(Unit)
    override fun toString(): String = "EmptyElement[$cont"
}

/**
 * Represents closed channel.
 * @suppress **This is unstable API and it is subject to change.**
 */
public class SpScClosed<in E>(
        @JvmField val closeCause: Throwable?
) {
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
