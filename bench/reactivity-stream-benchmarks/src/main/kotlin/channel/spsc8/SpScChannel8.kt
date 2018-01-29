package channel.spsc8

/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original License: https://github.com/JCTools/JCTools/blob/master/LICENSE
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/atomic/SpscAtomicArrayQueue.java
 */

import channel.Element
import java.util.concurrent.atomic.AtomicLongFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 *
 * Lock-free Single-Producer Single-Consumer Queue backed by a pre-allocated buffer.
 * Based on Ring Buffer = circular array = circular buffer
 * http://psy-lob-saw.blogspot.fr/2014/04/notes-on-concurrent-ring-buffer-queue.html
 * http://psy-lob-saw.blogspot.fr/2014/01/picking-2013-spsc-queue-champion.html
 * Inspirition https://www.codeproject.com/Articles/43510/Lock-Free-Single-Producer-Single-Consumer-Circular
 *
 * <p>
 * This implementation is a mashup of the <a href="http://sourceforge.net/projects/mc-fastflow/">Fast Flow</a>
 * algorithm with an optimization of the offer method taken from the <a
 * href="http://staff.ustc.edu.cn/~bhua/publications/IJPP_draft.pdf">BQueue</a> algorithm (a variation on Fast
 * Flow), and adjusted to comply with Queue.offer semantics with regards to capacity.<br>
 * For convenience the relevant papers are available in the resources folder:<br>
 * <i>2010 - Pisa - SPSC Queues on Shared Cache Multi-Core Systems.pdf<br>
 * 2012 - Junchang- BQueue- Efﬁcient and Practical Queuing.pdf <br>
 * </i> This implementation is wait free.
 *
 * @param <E> Not null value
 * @author nitsanw, adapted by fmo
 */
open class SpScChannel8<E : Any>(
        /**
         * Buffer capacity.
         */
        capacity: Int
) : SpscAtomicArrayQueueL5Pad8<Element<E>>(capacity) {

    private fun tryResumeReceive() {
        val empty = loGetAndSetNullEmpty()
        if (null != empty) {
//            println("Producer : tryResumeReceive -> Consumer is suspended, resume")
            empty.resume(Unit)
        }
    }

    private fun tryResumeSend() {
        val full = loGetAndSetNullFull()
        if (null != full) {
//            println("Consumer : tryResumeSend -> Producer is suspended, resume")
            full.resume(Unit)
        }
    }

    /**
     * Offer the value in buffer
     * Return true if there is room left in buffer, false if just 1 spot left in the buffer
     * <p>
     * This implementation is correct for single producer thread use only.
     */
    private fun offer(item: Element<E>, producerIndex: Long): Boolean {
        val mask = this.mask
        val offset = calcElementOffset(producerIndex, mask)
        if (!loCompareAndSetExpectedNullElement(offset, item)) return false
//        println("Producer : offer -> offer offset=$offset $item")
        if (null != item.closeCause) {
            tryResumeReceive()
            return true
        }
        // ordered store -> atomic and ordered for size()
        soLazyProducerIndex(producerIndex + 1)
        // handle empty case (= suspended Consumer)
        tryResumeReceive()
        return true
    }

    suspend fun send(item: Element<E>, prevProducerIndex: Long? = null) {
        val producerIndex = if (null!= prevProducerIndex) prevProducerIndex
        else lvProducerIndex()
        // fast path -- try offer non-blocking
        if (offer(item, producerIndex)) return
        // slow-path does suspend
        sendSuspend()
//        println("Producer : resume")
        send(item, producerIndex) // re-call send after suspension
    }

    private suspend fun sendSuspend(): Unit = suspendCoroutine { cont ->
        //        println("Producer : sendSuspend -> Producer suspend")
        soFull(cont)
        tryResumeReceive()
//        println("Producer : suspend")
    }

    private fun poll(consumerIndex: Long): E? {
        // local load of field to avoid repeated loads after volatile reads
        val offset = calcElementOffset(consumerIndex)
        val value = loGetAndSetNullElement(offset)
        if (null == value) {
            // empty buffer
//            println("Consumer : suspend because value is null offset=$offset")
            return null
        }
        // Check if Closed
        val closed = value.closeCause
        if (null != closed) {
//            println("Consumer : poll closed, offset = $offset, closed=$closed")
            throw closed
        }
//        println("Consumer : poll -> poll offset=$offset $value")
        // ordered store -> atomic and ordered for size()
        soLazyConsumerIndex(consumerIndex + 1)
        // we consumed the value from buffer, now check if Producer is full
        tryResumeSend()
//      println("receive ${value.item}")
        return value.item as E
    }

    /**
     * <p>
     * This implementation is correct for single producer thread use only.
     */
    suspend fun receive(prevConsumerIndex: Long? = null): E {
        val consumerIndex = if (null != prevConsumerIndex) prevConsumerIndex
        else lvConsumerIndex()
        // fast path -- try poll non-blocking
        val result = poll(consumerIndex)
        if (null != result) return result
        // slow-path does suspend
        receiveSuspend()
//        println("Consumer : resume")
        return receive(consumerIndex) // re-call receive after suspension
    }

    private suspend fun receiveSuspend(): Unit = suspendCoroutine { cont ->
        //        println("Consumer : receiveSuspend -> Consumer suspend")
        soEmpty(cont)
        tryResumeSend()
//        println("Consumer : suspend")
    }
}

abstract class AtomicReferenceArrayQueue8<E : Any>(capacity: Int) {
    @JvmField protected val buffer = AtomicReferenceArray<E?>(capacity)
    @JvmField protected val mask: Int = capacity - 1

    init {
        check(capacity > 0) { "capacity must be positive" }
        check(capacity and mask == 0) { "capacity must be a power of 2" }
    }

    protected fun calcElementOffset(index: Long) = index.toInt() and mask

    protected fun loCompareAndSetExpectedNullElement(offset: Int, value: E?) = buffer.compareAndSet(offset, null, value)

    protected fun loGetAndSetNullElement(offset: Int) = buffer.getAndSet(offset, null)

    protected companion object {
        @JvmStatic
        protected fun calcElementOffset(index: Long, mask: Int) = index.toInt() and mask
    }
}

abstract class SpscAtomicArrayQueueL1Pad8<E : Any>(capacity: Int) : AtomicReferenceArrayQueue8<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueProducerIndexFields8<E : Any>(capacity: Int) : SpscAtomicArrayQueueL1Pad8<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val P_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueProducerIndexFields8<*>>(SpscAtomicArrayQueueProducerIndexFields8::class.java, "producerIndex")
    @Volatile private var producerIndex: Long = 0L

    protected fun lvProducerIndex() = producerIndex
    protected fun soLazyProducerIndex(newValue: Long) { P_INDEX_UPDATER.lazySet(this, newValue) }
}

abstract class SpscAtomicArrayQueueL2Pad8<E : Any>(capacity: Int) : SpscAtomicArrayQueueProducerIndexFields8<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueConsumerIndexField8<E : Any>(capacity: Int) : SpscAtomicArrayQueueL2Pad8<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val C_INDEX_UPDATER  = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueConsumerIndexField8<*>>(SpscAtomicArrayQueueConsumerIndexField8::class.java, "consumerIndex")
    @Volatile private var consumerIndex: Long = 0L

    protected fun lvConsumerIndex() = consumerIndex
    protected fun soLazyConsumerIndex(newValue: Long) { C_INDEX_UPDATER.lazySet(this, newValue) }
}

abstract class SpscAtomicArrayQueueL3Pad8<E : Any>(capacity: Int) : SpscAtomicArrayQueueConsumerIndexField8<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class AtomicReferenceEmptyField8<E : Any>(capacity: Int) : SpscAtomicArrayQueueL3Pad8<E>(capacity) {
    private val EMPTY_UPDATER = AtomicReferenceFieldUpdater.newUpdater<AtomicReferenceEmptyField8<*>, Continuation<*>>(AtomicReferenceEmptyField8::class.java,
            Continuation::class.java, "empty")
    @Volatile private var empty: Continuation<Unit>? = null

    internal fun loGetAndSetNullEmpty() = EMPTY_UPDATER.getAndSet(this, null) as Continuation<Unit>?
    internal fun soEmpty(value: Continuation<Unit>) { EMPTY_UPDATER.set(this, value) }
}

abstract class SpscAtomicArrayQueueL4Pad8<E : Any>(capacity: Int) : AtomicReferenceEmptyField8<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class AtomicReferenceFullField8<E : Any>(capacity: Int) : SpscAtomicArrayQueueL4Pad8<E>(capacity) {
    private val FULL_UPDATER = AtomicReferenceFieldUpdater.newUpdater<AtomicReferenceFullField8<*>, Continuation<*>>(AtomicReferenceFullField8::class.java,
            Continuation::class.java, "full")
    @Volatile private var full: Continuation<Unit>? = null

    internal fun loGetAndSetNullFull(): Continuation<Unit>? = FULL_UPDATER.getAndSet(this, null) as Continuation<Unit>?
    internal fun soFull(value: Continuation<Unit>) { FULL_UPDATER.set(this, value) }
}

abstract class SpscAtomicArrayQueueL5Pad8<E : Any>(capacity: Int) : AtomicReferenceFullField8<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
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
//        private class EmptyHasNext<E : Any>(
//                @JvmField val iterator: SpScChannel.Itr<E>,
//                @JvmField val cont: CancellableContinuation<Boolean>?
//        ) : Empty<E> {
//            override fun resumeReceive(element: E) {
//                iterator.result = element
//                cont?.resume(true)
//            }
//        }
//    }
//
//fun cancel(cause: Throwable? = null) =
//        close(cause).let {
//            cleanupSendQueueOnCancel()
//        }
//
//private fun cleanupSendQueueOnCancel() { // todo cleanup for Garbage Collector
//}
//
///**
// * Makes sure that the given [block] consumes all elements from the given channel
// * by always invoking [cancel][SpScChannel.cancel] after the execution of the block.
// */
//public inline fun <E : Any, R> SpScChannel2<E>.consume(block: SpScChannel<E>.() -> R): R =
//        try {
//            block()
//        } finally {
//            cancel()
//        }
//
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
