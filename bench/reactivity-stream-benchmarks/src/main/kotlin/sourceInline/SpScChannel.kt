package sourceInline

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.loop
import kotlinx.atomicfu.update
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Inspired by https://www.codeproject.com/Articles/43510/Lock-Free-Single-Producer-Single-Consumer-Circular
 */
public open class SpScChannel<E : Any>(
        /**
         * Buffer capacity.
         */
        capacity: Int
) : SpscAtomicArrayQueueL3Pad<Element<E>>(capacity), Sink<E> {

    protected val _full = atomic<Suspended?>(null)
    protected val _empty = atomic<Suspended?>(null)

    /**
     * Offer the value in buffer
     * Return true if there is room left in buffer, false if just 1 spot left in the buffer
     */
    fun offer(item: E): Boolean {
        println("offer $item")
        // local load of field to avoid repeated loads after volatile reads
        val buffer = this.buffer
        val mask = this.mask
        val producerIndex = lvProducerIndex()
        val offset = calcElementOffset(producerIndex, mask)
        // StoreStore
        soElement(buffer, offset, Element(item))
        // ordered store -> atomic and ordered for size()
        soProducerIndex(producerIndex + 1)
        // if empty = suspended Consumer, then no check required for full buffer
        if (handleEmpty()) return true
        // return true if buffer has only one last free spot (we leave one spot remaining for Closed item)
        return offset != calcElementOffset(lvConsumerIndex() - 2)
    }

    /**
     * Return true if empty = suspended Consumer must resume
     */
    private fun handleEmpty(): Boolean {
        _empty.loop {empty ->
            println("handleEmpty $empty")
            empty?.resume() ?: return false
            _empty.lazySet(null)
            return true
        }
    }

    final override suspend fun send(item: E) {
        // fast path -- try offer non-blocking
        if (offer(item)) return
        // slow-path does suspend
        return sendSuspend(item)
    }

    private suspend fun sendSuspend(element: E): Unit = suspendCoroutine { cont ->
        println("sendSuspend")
        val full = Suspended(cont)
        _full.update {
            full
        }
        //        cont.invokeOnCompletion { // todo test without first and then try it with a Unit test that Cancels
//            soFull(null)
//        }
    }

    override fun close(cause: Throwable?) {
        val buffer = this.buffer
        val mask = this.mask
        val producerIndex = lvProducerIndex()
        val offset = calcElementOffset(producerIndex, mask)
        // StoreStore
        val closeCause = cause ?: ClosedReceiveChannelException(DEFAULT_CLOSE_MESSAGE)
        println("close $cause offset = $offset")
        soElement(buffer, offset, Element(closeCause = closeCause))
    }

    /**
     * Full element to store when suspend Producer or Consumer
     */
    protected class Suspended(
            @JvmField var cont: Continuation<Unit>?
    ) {
        fun resume() { cont?.resume(Unit) }
        override fun toString() = "FullElement[$cont]"
    }
//}
//
//class SpScChannel<E : Any>(capacity: Int) : SpScSendChannel<E>(capacity) {

    private fun handleFull(): Boolean {
        _full.loop {full ->
            full?.resume() ?: return false
            _full.lazySet(null)
            return true
        }
    }

    suspend fun receive(): E {
        println("receive")
        // local load of field to avoid repeated loads after volatile reads
        val buffer = this.buffer
        val consumerIndex = lvConsumerIndex()
        val offset = calcElementOffset(consumerIndex)
        // LoadLoad
        val value = lvElement(buffer, offset)
        if (null == value) {
            receiveSuspend()
            return receive() // re-call after suspension
        } else {
            if (null != value.closeCause) throw value.closeCause // fail fast
            // StoreStore
            soElement(buffer, offset, null)
            // ordered store -> atomic and ordered for size()
            soConsumerIndex(consumerIndex + 1)
            // we consumed the value from buffer, now check if Producer is full
            if (handleFull()) return value.item as E // if producer was full, no need to check if consumer is empty
        }
        // if buffer is empty -> slow-path does suspend
        if (offset == calcElementOffset(lvProducerIndex() - 1)) receiveSuspend()
        return value.item as E
    }

    private suspend fun receiveSuspend(): Unit = suspendCoroutine { cont ->
        println("receiveSuspend")
        val empty = Suspended(cont)
        _empty.update {
            empty
        }
        //        cont.invokeOnCompletion { // todo test without first and then try it with a Unit test that Cancels parent
//            _empty.lazySet(null)
//        }
    }

    fun cancel(cause: Throwable? = null) =
            close(cause).let {
                cleanupSendQueueOnCancel()
            }

    private fun cleanupSendQueueOnCancel() { // todo cleanup for Garbage Collector
    }

//    public final operator fun iterator(): ChannelIterator<E> = Itr(this)
//
//    private class Itr<E : Any>(val channel: SpScChannel<E>) : ChannelIterator<E> {
//        var result: E? = null
//
//        override suspend fun hasNext(): Boolean {
//            // check for repeated hasNext
//            if (result != null) return hasNextResult(result)
//            // fast path -- try poll non-blocking
//            result = channel.poll()
//            if (hasNextResult(result)) return true
//            // slow-path does suspend
//            return hasNextSuspend()
//        }
//
//        private fun hasNextResult(result: Any?): Boolean {
//            if (null != result) {
//                if (result == 119) println("polled $result")
//                // handle the potentially suspended consumer (full buffer), one slot is free now in buffer
//                channel.handleFull()
//                return true
//            } else {
//                // maybe there is a Closed event
//                println("no result in buffer, try to handle closed")
//                channel.handleClosed()
//                return false
//            }
//        }
//
//        private suspend fun hasNextSuspend(): Boolean = suspendCancellableCoroutine(holdCancellability = true) sc@ { cont ->
//            println("hasNextSuspend")
//            val empty = EmptyHasNext(this, cont)
//            channel.soEmpty(empty)
//            //        cont.invokeOnCompletion { // todo test without first and then try it with a Unit test that Cancels parent
////            _empty.lazySet(null)
////        }
//        }
//
//        @Suppress("UNCHECKED_CAST")
//        override suspend fun next(): E {
//            val result = result
//            if (null != result) {
//                this.result = null
//                return result
//            }
//            // rare case when hasNext was not invoked yet -- just delegate to receive (leave state as is)
//            return channel.receive()
//
//        }
//
//        protected class EmptyHasNext<E : Any>(
//                @JvmField val iterator: SpScChannel.Itr<E>,
//                @JvmField val cont: CancellableContinuation<Boolean>?
//        ) : Empty<E> {
//            override fun resumeReceive(element: E) {
//                iterator.result = element
//                cont?.resume(true)
//            }
//        }
//    }
}

/**
 * Makes sure that the given [block] consumes all elements from the given channel
 * by always invoking [cancel][SpScChannel.cancel] after the execution of the block.
 */
public inline fun <E : Any, R> SpScChannel<E>.consume(block: SpScChannel<E>.() -> R): R =
        try {
            block()
        } finally {
            cancel()
        }

///**
// * Performs the given [action] for each received element.
// *
// * This function [consumes][consume] all elements of the original [SpScChannel].
// */
//public inline suspend fun <E : Any> SpScChannel<E>.consumeEach(action: (E) -> Unit) =
//            try {
//                for (element in this) action(element)
//            } catch (e: Throwable) {
//                if (e !is ClosedReceiveChannelException) cancel(e)
//                else cancel()
//            } finally {
//                cancel()
//            }

private const val DEFAULT_CLOSE_MESSAGE = "SpScChannel was closed"

/**
 * Element stored in the buffer
 */
data class Element<E : Any>(
        val item: E? = null,
        val closeCause: Throwable? = null
)

class BufferShouldNotBeEmptyException: IndexOutOfBoundsException("Buffer should not be empty")
