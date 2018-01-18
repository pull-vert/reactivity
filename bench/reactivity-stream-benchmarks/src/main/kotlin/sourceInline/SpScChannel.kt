package sourceInline

import internal.LockFreeSPSCQueue
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.channels.ChannelIterator
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import kotlinx.coroutines.experimental.channels.POLL_FAILED
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
        fun resumeSend(){
            cont?.resume(Unit)
        }
        override fun toString() = "FullElement($offerValue)[$cont]"
    }

    protected interface Empty<in E : Any> {
        fun resumeReceive(element: E)
    }

    /**
     * Empty element to store when suspend Consumer
     */
    protected open class EmptyElement<in E : Any>(
            @JvmField val cont: CancellableContinuation<E>?
    ): Empty<E> {
        override fun resumeReceive(element: E) {
            cont?.resume(element)
        }
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
    private val EMPTY_UPDATER = AtomicReferenceFieldUpdater.newUpdater<SpScSendChannel<*>, Empty<*>>(
            SpScSendChannel::class.java, Empty::class.java, "empty")
    @Volatile @JvmField protected var empty: Empty<E>? = null
    private val CLOSED_UPDATER = AtomicReferenceFieldUpdater.newUpdater<SpScSendChannel<*>, ClosedElement>(
            SpScSendChannel::class.java, ClosedElement::class.java, "closed")
    @Volatile @JvmField protected var closed: ClosedElement? = null

    protected fun soFull(newValue: FullElement<E>?) = FULL_UPDATER.lazySet(this, newValue)

    protected fun soEmpty(newValue: Empty<E>?) = EMPTY_UPDATER.lazySet(this, newValue)

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

    private fun pollInternal(): E? = queue.poll()

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

        public final fun iterator(): ChannelIterator<E> = Itr(this)

    private class Itr<E : Any>(val channel: SpScChannel<E>) : ChannelIterator<E> {
        var result: E? = null

        override suspend fun hasNext(): Boolean {
            // check for repeated hasNext
            if (result != null) return hasNextResult(result)
            // fast path -- try poll non-blocking
            result = channel.pollInternal()
            if (result != null) return hasNextResult(result)
            // slow-path does suspend
            return hasNextSuspend()
        }

        private fun hasNextResult(result: Any?): Boolean {
            if (null != result) {
                if (result === 119) println("polled $result")
                // handle the potentially suspended consumer (full buffer), one slot is free now in buffer
                channel.handleFull()
                return true
            } else {
                // maybe there is a Closed event
                channel.handleClosed()
                return false
            }
        }

        private suspend fun hasNextSuspend(): Boolean = suspendCancellableCoroutine(holdCancellability = true) sc@ { cont ->
            val empty = EmptyHasNext(this, cont)
            channel.soEmpty(empty)
            //        cont.invokeOnCompletion { // todo test without first and then try it with a Unit test that Cancels parent
//            _empty.lazySet(null)
//        }
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

        protected class EmptyHasNext<E : Any>(
                @JvmField val iterator: SpScChannel.Itr<E>,
                @JvmField val cont: CancellableContinuation<Boolean>?
        ): Empty<E> {
            override fun resumeReceive(element: E) {
                iterator.result = element
                cont?.resume(true)
            }
        }
    }
}

internal const val DEFAULT_CLOSE_MESSAGE = "SpScChannel was closed"
