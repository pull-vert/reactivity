package sourceInline

import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import reactivity.experimental.intrinsics.LockFreeSPSCQueue

public abstract class AbstractSpScSendChannel<E : Any>(
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
    protected open fun offerInternal(element: E): Any {
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
        val send = SpScSendElement(element, queue.nextValueToConsume(), cont)
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
    private fun enqueueSend(send: SpScSendElement<Any?>): Any? {
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
        val result = queue.offer(closed)
        when {
            result === OFFER_SUCCESS -> return
            result === OFFER_FAILED -> // continue
            result is SpScClosed<*> -> throw result.sendException
            else -> error("offerInternal returned $result")
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
