package sourceInline

import internal.LockFreeSPSCQueue
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

public open class SpScSendChannel<E : Any>(
        /**
         * Buffer capacity.
         */
        capacity: Int
) : Sink<E> {

    /**
     * Full element to store when suspend Producer
     */
    protected class FullElement<E : Any>(
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
    protected class EmptyElement<in E>(
            @JvmField val cont: CancellableContinuation<E>?
    ) {
        fun resumeReceive(element: E) = cont?.resume(element)
        override fun toString(): String = "EmptyElement[$cont"
    }

    /**
     * Closed element to store when Producer is closed
     */
    protected class ClosedElement(
            private val closeCause: Throwable?
    ) {
        val receiveException: Throwable get() = closeCause ?: ClosedReceiveChannelException(DEFAULT_CLOSE_MESSAGE)
        override fun toString() = "Closed[$closeCause]"
    }

    @JvmField protected val queue = LockFreeSPSCQueue<E>(capacity)
    private val FULL_UPDATER = AtomicReferenceFieldUpdater.newUpdater<SpScSendChannel<*>, FullElement<*>>(
            SpScSendChannel::class.java, FullElement::class.java, "full")
    @Volatile @JvmField protected var full: FullElement<E>? = null
    private val EMPTY_UPDATER = AtomicReferenceFieldUpdater.newUpdater<SpScSendChannel<*>, EmptyElement<*>>(
            SpScSendChannel::class.java, EmptyElement::class.java, "empty")
    @Volatile @JvmField protected var empty: EmptyElement<E>? = null
    private val CLOSED_UPDATER = AtomicReferenceFieldUpdater.newUpdater<SpScSendChannel<*>, ClosedElement>(
            SpScSendChannel::class.java, ClosedElement::class.java, "closed")
    @Volatile @JvmField protected var closed: ClosedElement? = null

    protected fun soFull(newValue: FullElement<E>?) = FULL_UPDATER.lazySet(this, newValue)

    protected fun soEmpty(newValue: EmptyElement<E>?) = EMPTY_UPDATER.lazySet(this, newValue)

    protected fun soClosed(newValue: ClosedElement?) = CLOSED_UPDATER.lazySet(this, newValue)

    private fun handleEmpty(element: E): Boolean {
        val empty = empty
        empty?.resumeReceive(element) ?: return false
        soEmpty(null)
        return true
    }

    public final override suspend fun send(item: E) {
        // handle the potentially suspended consumer (empty buffer), there is at least one element in buffer
        if (handleEmpty(item)) return
        // fast path -- try offer non-blocking
        if (queue.offer(item)) return
        // slow-path does suspend
        return sendSuspend(item)
    }

    private suspend fun sendSuspend(element: E): Unit = suspendCancellableCoroutine { cont ->
        val full = FullElement(element, cont)
        soFull(full) //  store full in atomic _full
        //        cont.invokeOnCompletion { // todo test without first and then try it with a Unit test that Cancels
//            soFull(null)
//        }
    }

    public override fun close(cause: Throwable?) = soClosed(ClosedElement(cause))
}

class SpScChannel<E : Any>(capacity: Int) : SpScSendChannel<E>(capacity) {

    private fun handleClosed() {
        val exception = closed?.receiveException ?: return
        soClosed(null)
        throw exception
    }

    private fun handleFull() {
        full?.resumeSend() ?: return
        soFull(null)
    }

    public final suspend fun receive(): E {
        // fast path -- try poll non-blocking
        val value = queue.poll()
        if (null != value) {
            if (value === 119) println("polled $value")
            // handle the potentially suspended consumer (full buffer), one slot is free now in buffer
            handleFull()
            return value
        } else {
            // maybe there is a Closed event
            handleClosed()
            // buffer is empty -> slow-path does suspend
            return receiveSuspend()
        }
    }

    private suspend fun receiveSuspend(): E = suspendCancellableCoroutine { cont ->
        val empty = EmptyElement(cont)
        soEmpty(empty)
        //        cont.invokeOnCompletion { // todo test without first and then try it with a Unit test that Cancels parent
//            _empty.lazySet(null)
//        }
    }
}

internal const val DEFAULT_CLOSE_MESSAGE = "SpScChannel was closed"

//    public final fun iterator(): ChannelIterator<E> = Itr(this)
//
//    private class Itr<E : Any>(val channel: SpScChannel<E>) : ChannelIterator<E> {
//        var result: Any? = POLL_FAILED // E | POLL_FAILED | Closed
//
//        override suspend fun hasNext(): Boolean {
//            // check for repeated hasNext
//            if (result !== POLL_FAILED) return hasNextResult(result)
//            // fast path -- try poll non-blocking
//            result = channel.pollInternal()
//            if (result !== POLL_FAILED) return hasNextResult(result)
//            // slow-path does suspend
//            return hasNextSuspend()
//        }
//
//        private fun hasNextResult(result: Any?): Boolean {
//            if (result is Closed<*>) {
//                if (result.closeCause != null) throw result.receiveException
//                return false
//            }
//            return true
//        }
//
//        private suspend fun hasNextSuspend(): Boolean = suspendCancellableCoroutine(holdCancellability = true) sc@ { cont ->
//            val receive = ReceiveHasNext(this, cont)
//            while (true) {
//                if (channel.enqueueReceive(receive)) {
//                    cont.initCancellability() // make it properly cancellable
//                    channel.removeReceiveOnCancel(cont, receive)
//                    return@sc
//                }
//                // hm... something is not right. try to poll
//                val result = channel.pollInternal()
//                this.result = result
//                if (result is Closed<*>) {
//                    if (result.closeCause == null)
//                        cont.resume(false)
//                    else
//                        cont.resumeWithException(result.receiveException)
//                    return@sc
//                }
//                if (result !== POLL_FAILED) {
//                    cont.resume(true)
//                    return@sc
//                }
//            }
//        }
//
//        @Suppress("UNCHECKED_CAST")
//        override suspend fun next(): E {
//            val result = this.result
//            if (result is Closed<*>) throw result.receiveException
//            if (result !== POLL_FAILED) {
//                this.result = POLL_FAILED
//                return result as E
//            }
//            // rare case when hasNext was not invoked yet -- just delegate to receive (leave state as is)
//            return channel.receive()
//        }
//    }
