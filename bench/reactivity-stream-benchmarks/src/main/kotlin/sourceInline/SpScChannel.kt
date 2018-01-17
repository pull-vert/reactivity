package sourceInline

import internal.LockFreeSPSCQueue
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.suspendCancellableCoroutine

public open class SpScSendChannel<E : Any>(
        /**
         * Buffer capacity.
         */
        capacity: Int
) : Sink<E> {

    @JvmField protected val queue = LockFreeSPSCQueue<E>(capacity)
    internal val _full = atomic<FullElement<E>?>(null)
    internal val _empty = atomic<EmptyElement<E>?>(null)
    internal val _closed = atomic<ClosedElement?>(null)

    private fun handleEmpty(element: E): Boolean {
        _empty.value?.resumeReceive(element) ?: return false
        _empty.lazySet(null)
        return true
    }

    public final override suspend fun send(item: E) {
        // handle the potentially suspended consumer (empty buffer)
        if (handleEmpty(item)) return
        // fast path -- try offer non-blocking
        if (queue.offer(item)) return
        // slow-path does suspend
        return sendSuspend(item)
    }

    private suspend fun sendSuspend(element: E): Unit = suspendCancellableCoroutine sc@ { cont ->
        val full = FullElement(element, cont)
        _full.lazySet(full) //  store full in atomic _full
//        cont.invokeOnCompletion { // todo test without first and then try it with a Unit test that Cancels parent
//            _full.lazySet(null)
//        }
    }

    public override fun close(cause: Throwable?) {
        _closed.lazySet(ClosedElement(cause))
    }
}

class SpScChannel<E : Any>(capacity: Int) : SpScSendChannel<E>(capacity) {

    private fun handleClosed() {
        val exception = _closed.value?.receiveException ?: return
        _closed.lazySet(null)
        throw exception
    }

    private fun handleFull() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Suppress("UNCHECKED_CAST")
    public final suspend fun receive(): E {
        // fast path -- try poll non-blocking
        var value = queue.poll()
        if (null != value) {
            // handle the potentially suspended consumer (full buffer)
            handleFull()
            return value
        } else {
            // maybe there is a Closed event
            handleClosed()
            // buffer is empty -> slow-path does suspend
            return receiveSuspend()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun receiveSuspend(): E = suspendCancellableCoroutine(holdCancellability = true) sc@ { cont ->
        val receive = EmptyElement(cont)
        while (true) {
            if (enqueueReceive(receive)) {
                cont.initCancellability() // make it properly cancellable
//                removeReceiveOnCancel(cont, receive)
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

    private fun enqueueReceive(receive: EmptyElement<E>): Boolean {
        val result = if (isBufferAlwaysEmpty)
            queue.addLastIfPrev(receive, { it !is Send }) else
            queue.addLastIfPrevAndIf(receive, { it !is Send }, { isBufferEmpty })
        if (result) onEnqueuedReceive()
        return result
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
internal class FullElement<E : Any>(
        val offerValue: E,
        @JvmField var cont: CancellableContinuation<Unit>?
) {
    lateinit var currentValue: Any
    fun resumeSend() = cont?.resume(Unit)
    override fun toString() = "FullElement($offerValue)[$cont]"
}

/**
 * Empty element to store when suspend Consumer
 */
internal class EmptyElement<in E>(
        @JvmField val cont: CancellableContinuation<E>?
) {
    fun resumeReceive(element: E) = cont?.resume(element)
    override fun toString(): String = "EmptyElement[$cont"
}

/**
 * Closed element to store when Producer is closed
 */
internal class ClosedElement(
        private val closeCause: Throwable?
) {
    val receiveException: Throwable get() = closeCause ?: ClosedReceiveChannelException(DEFAULT_CLOSE_MESSAGE)
    override fun toString() = "Closed[$closeCause]"
}
