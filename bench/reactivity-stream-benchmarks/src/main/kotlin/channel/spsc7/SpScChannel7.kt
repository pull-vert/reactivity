package channel.spsc7

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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import java.util.concurrent.atomic.AtomicLongFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.math.min

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
public open class SpScChannel7<E : Any>(
        /**
         * Buffer capacity.
         */
        capacity: Int
) : SpscAtomicArrayQueueL10Pad6<Element<E>>(capacity) {

    private fun tryResumeReceive() {
        val emptyBuffer = this.emptyBuffer
        val mask = this.mask
        val emptyConsumerIndex = lvEmptyConsumerIndex()
        val offset = calcElementOffset(emptyConsumerIndex, mask)
        // LoadLoad
        val empty = lvEmpty(emptyBuffer, offset)
        if (null != empty) {
            println("Producer : tryResumeReceive -> Consumer is suspended, resume")
            soSuspendFlag(NO_SUSPEND)
            empty.resume(Unit)
            soEmpty(emptyBuffer, offset, null)
            soEmptyConsumerIndex(emptyConsumerIndex + 1)
        }
    }

    /**
     * Called when Consumer is or will be suspended
     */
    private fun resumeReceive() {
        // Consumer is suspended
//        println("Producer : resumeReceive -> START while loop")
        val mask = this.mask
        val emptyConsumerIndex = lvEmptyConsumerIndex()
        val offset = calcElementOffset(emptyConsumerIndex, mask)
        while(true) {
            val empty = loGetAndSetNullEmpty(offset)
            if (null != empty) {
                println("Producer : resumeReceive -> EXIT while loop")
                empty.resume(Unit)
                soEmpty(emptyBuffer, offset, null)
                soEmptyConsumerIndex(emptyConsumerIndex + 1)
                return
            }
        }
    }

    private fun tryResumeSend() {
        val fullBuffer = this.fullBuffer
        val mask = this.mask
        val fullConsumerIndex = lvFullConsumerIndex()
        val offset = calcElementOffset(fullConsumerIndex, mask)
        // LoadLoad
        val full = lvFull(fullBuffer, offset)
        if (null != full) {
            println("Consumer : tryResumeSend -> Producer is suspended, resume")
            soSuspendFlag(NO_SUSPEND)
            full.resume(Unit)
            soFull(fullBuffer, offset, null)
            soFullConsumerIndex(fullConsumerIndex + 1)
        }
    }

    /**
     * Called when Producer is or will be suspended
     */
    private fun resumeSend() {
        // Producer is suspended
//        println("Consumer : resumeSend -> START while loop")
        val mask = this.mask
        val fullConsumerIndex = lvFullConsumerIndex()
        val offset = calcElementOffset(fullConsumerIndex, mask)
        while(true) {
            val full = loGetAndSetNullFull(offset)
            if (null != full) {
                println("Consumer : resumeSend -> EXIT while loop")
                full.resume(Unit)
                soFull(fullBuffer, offset, null)
                soFullConsumerIndex(fullConsumerIndex + 1)
                return
            }
        }
    }

    /**
     * Offer the value in buffer
     * Return true if there is room left in buffer, false if just 1 spot left in the buffer
     * <p>
     * This implementation is correct for single producer thread use only.
     */
    private fun offer(item: Element<E>): Boolean {
        // local load of field to avoid repeated loads after volatile reads
        val mask = this.mask
        val producerIndex = lvProducerIndex()
        val offset = calcElementOffset(producerIndex, mask)
        soElement(offset, item)
//        println("Producer : offer -> produced at offset=$offset")
        if (null != item.closeCause) {
            if (EMPTY_SUSPEND == loGetAndSetSuspendFlag(NO_SUSPEND)) resumeReceive()
            return true
        }
        // ordered store -> atomic and ordered for size()
        soLazyProducerIndex(producerIndex + 1)
        // check if buffer is full
        if (producerIndex >= producerLimit && !hasRoomLeft(buffer, mask, producerIndex)) {
            return false
        }
        // handle empty case (= suspended Consumer)
//        tryResumeReceive()
        return true
    }

    private fun hasRoomLeft(buffer: AtomicReferenceArray<Element<E>?>, mask: Int, producerIndex: Long): Boolean {
        val lookAheadStep = this.lookAheadStep
        if (null == lvElement(buffer, calcElementOffset(producerIndex + lookAheadStep, mask))) {
            producerLimit = producerIndex + lookAheadStep
        } else {
            val offsetNext = calcElementOffset(producerIndex + 1, mask)
            val nextEl = lvElement(buffer, offsetNext)
            if (null != nextEl) println("Producer : suspend because value is not null offset=$offsetNext")
            return null == nextEl
        }
        return true
    }

    suspend fun send(item: Element<E>) {
        // fast path -- try offer non-blocking
        if (offer(item)) return
        // slow-path does suspend
        sendSuspend()
        println("Producer : resume")
    }

    private suspend fun sendSuspend(): Unit = suspendCoroutine { cont ->
        if (EMPTY_SUSPEND == loGetAndSetSuspendFlag(FULL_SUSPEND)) resumeReceive() // notify Producer will Suspend
//        println("Producer : sendSuspend -> Producer suspend")
        // local load of field to avoid repeated loads after volatile reads
        val fullBuffer = this.fullBuffer
        val fullProducerIndex = lvFullProducerIndex()
        val offset = calcElementOffset(fullProducerIndex)
        // ordered store -> atomic and ordered for size()
        soFull(fullBuffer, offset, cont)
        soFullProducerIndex(fullProducerIndex + 1)
        println("Producer : suspend")
    }

    private fun poll(): E? {
        // local load of field to avoid repeated loads after volatile reads
//        val buffer = this.buffer
        val consumerIndex = lvConsumerIndex()
        val offset = calcElementOffset(consumerIndex)
        val value = loGetAndSetNullElement(offset)
        if (null == value) {
            println("Consumer : suspend because value is null offset=$offset")
            return null
        }
        // empty buffer
//        soElement(buffer, offset, null)
        // before suspend, check if Closed
        val closed = value.closeCause
        if (null != closed) {
            println("Consumer : poll closed, offset = $offset, closed=$closed")
            throw closed
        }
        println("Consumer : poll -> poll offset=$offset $value")
        // ordered store -> atomic and ordered for size()
        soLazyConsumerIndex(consumerIndex + 1)
        // we consumed the value from buffer, now check if Producer is full
//        tryResumeSend()
//      println("receive ${value.item}")
        return value.item as E
    }

    /**
     * <p>
     * This implementation is correct for single producer thread use only.
     */
    suspend fun receive(): E {
        // fast path -- try poll non-blocking
        val result = poll()
        if (null != result) return result
        // slow-path does suspend
        receiveSuspend()
        println("Consumer : resume")

        return receive() // re-call receive after suspension
    }

    private suspend fun receiveSuspend(): Unit = suspendCoroutine { cont ->
        if (FULL_SUSPEND == loGetAndSetSuspendFlag(EMPTY_SUSPEND)) resumeSend() // notify Consumer will Suspend
//        println("Consumer : receiveSuspend -> Consumer suspend")
        // local load of field to avoid repeated loads after volatile reads
        val emptyBuffer = this.emptyBuffer
        val emptyProducerIndex = lvEmptyProducerIndex()
        val offset = calcElementOffset(emptyProducerIndex)
        // ordered store -> atomic and ordered for size()
        soEmpty(emptyBuffer, offset, cont)
        soEmptyProducerIndex(emptyProducerIndex + 1)
        println("Consumer : suspend")
    }
}

private const val NO_SUSPEND = 0
private const val FULL_SUSPEND = 1
private const val EMPTY_SUSPEND = -1

abstract class AtomicReferenceArrayQueue6<E : Any>(capacity: Int) {
    @JvmField protected val buffer = AtomicReferenceArray<E?>(capacity)
    @JvmField protected val mask: Int = capacity - 1

    init {
        check(capacity > 0) { "capacity must be positive" }
        check(capacity and mask == 0) { "capacity must be a power of 2" }
    }

    protected fun calcElementOffset(index: Long) = index.toInt() and mask

    protected fun loGetAndSetNullElement(offset: Int) = buffer.getAndSet(offset, null)

    protected fun soElement(offset: Int, value: E?) { buffer.set(offset, value) }

    protected companion object {
        @JvmStatic
        protected fun <E : Any> lvElement(buffer: AtomicReferenceArray<E?>, offset: Int) = buffer.get(offset)

        @JvmStatic
        protected fun calcElementOffset(index: Long, mask: Int) = index.toInt() and mask
    }
}

abstract class SpscAtomicArrayQueueColdField6<E : Any>(capacity: Int) : AtomicReferenceArrayQueue6<E>(capacity) {
    @JvmField protected val lookAheadStep: Int

    init {
        lookAheadStep = min(capacity / 4, MAX_LOOK_AHEAD_STEP)
    }

    companion object {
        val MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096)
    }
}

abstract class SpscAtomicArrayQueueL1Pad6<E : Any>(capacity: Int) : SpscAtomicArrayQueueColdField6<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueProducerIndexFields6<E : Any>(capacity: Int) : SpscAtomicArrayQueueL1Pad6<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val P_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueProducerIndexFields6<*>>(SpscAtomicArrayQueueProducerIndexFields6::class.java, "producerIndex")
    @Volatile private var producerIndex: Long = 0L
    @JvmField protected var producerLimit: Long = 0L

    protected fun lvProducerIndex() = producerIndex
    protected fun soLazyProducerIndex(newValue: Long) { P_INDEX_UPDATER.lazySet(this, newValue) }
}

abstract class SpscAtomicArrayQueueL2Pad6<E : Any>(capacity: Int) : SpscAtomicArrayQueueProducerIndexFields6<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueConsumerIndexField6<E : Any>(capacity: Int) : SpscAtomicArrayQueueL2Pad6<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val C_INDEX_UPDATER  = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueConsumerIndexField6<*>>(SpscAtomicArrayQueueConsumerIndexField6::class.java, "consumerIndex")
    @Volatile private var consumerIndex: Long = 0L

    protected fun lvConsumerIndex() = consumerIndex
    protected fun soLazyConsumerIndex(newValue: Long) { C_INDEX_UPDATER.lazySet(this, newValue) }
}

abstract class SpscAtomicArrayQueueL3Pad6<E : Any>(capacity: Int) : SpscAtomicArrayQueueConsumerIndexField6<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class AtomicReferenceArrayEmpty6<E : Any>(capacity: Int) : SpscAtomicArrayQueueL3Pad6<E>(capacity) {
    @JvmField internal val emptyBuffer = AtomicReferenceArray<Continuation<Unit>?>(capacity)

    // todo remove if possible
    internal fun loGetAndSetNullEmpty(offset: Int) = emptyBuffer.getAndSet(offset, null)

    protected companion object {
        @JvmStatic
        internal fun lvEmpty(emptyBuffer: AtomicReferenceArray<Continuation<Unit>?>, offset: Int) = emptyBuffer.get(offset)

        @JvmStatic
        internal fun soEmpty(emptyBuffer: AtomicReferenceArray<Continuation<Unit>?>, offset: Int, value: Continuation<Unit>?) = emptyBuffer.lazySet(offset, value)
    }
}

abstract class SpscAtomicArrayQueueL4Pad6<E : Any>(capacity: Int) : AtomicReferenceArrayEmpty6<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueEmptyProducerIndexField6<E : Any>(capacity: Int) : SpscAtomicArrayQueueL4Pad6<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val E_P_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueEmptyProducerIndexField6<*>>(SpscAtomicArrayQueueEmptyProducerIndexField6::class.java, "emptyProducerIndex")
    @Volatile private var emptyProducerIndex: Long = 0L

    protected fun lvEmptyProducerIndex() = emptyProducerIndex
    protected fun soEmptyProducerIndex(newValue: Long) = E_P_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL5Pad6<E : Any>(capacity: Int) : SpscAtomicArrayQueueEmptyProducerIndexField6<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueEmptyConsumerIndexField6<E : Any>(capacity: Int) : SpscAtomicArrayQueueL5Pad6<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val E_C_INDEX_UPDATER  = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueEmptyConsumerIndexField6<*>>(SpscAtomicArrayQueueEmptyConsumerIndexField6::class.java, "emptyConsumerIndex")
    @Volatile private var emptyConsumerIndex: Long = 0

    protected fun lvEmptyConsumerIndex() = emptyConsumerIndex
    protected fun soEmptyConsumerIndex(newValue: Long) = E_C_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL6Pad6<E : Any>(capacity: Int) : SpscAtomicArrayQueueEmptyConsumerIndexField6<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class AtomicReferenceArrayFull6<E : Any>(capacity: Int) : SpscAtomicArrayQueueL6Pad6<E>(capacity) {
    @JvmField internal val fullBuffer = AtomicReferenceArray<Continuation<Unit>?>(capacity)

    // todo remove if possible
    internal fun loGetAndSetNullFull(offset: Int) = fullBuffer.getAndSet(offset, null)

    protected companion object {
        @JvmStatic
        internal fun lvFull(fullBuffer: AtomicReferenceArray<Continuation<Unit>?>, offset: Int) = fullBuffer.get(offset)

        @JvmStatic
        internal fun soFull(fullBuffer: AtomicReferenceArray<Continuation<Unit>?>, offset: Int, value: Continuation<Unit>?) = fullBuffer.lazySet(offset, value)
    }
}

abstract class SpscAtomicArrayQueueL7Pad6<E : Any>(capacity: Int) : AtomicReferenceArrayFull6<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueFullProducerIndexField6<E : Any>(capacity: Int) : SpscAtomicArrayQueueL7Pad6<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val F_P_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueFullProducerIndexField6<*>>(SpscAtomicArrayQueueFullProducerIndexField6::class.java, "fullProducerIndex")
    @Volatile private var fullProducerIndex: Long = 0L

    protected fun lvFullProducerIndex() = fullProducerIndex
    protected fun soFullProducerIndex(newValue: Long) = F_P_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL8Pad6<E : Any>(capacity: Int) : SpscAtomicArrayQueueFullProducerIndexField6<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueFullConsumerIndexField6<E : Any>(capacity: Int) : SpscAtomicArrayQueueL8Pad6<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val F_C_INDEX_UPDATER  = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueFullConsumerIndexField6<*>>(SpscAtomicArrayQueueFullConsumerIndexField6::class.java, "fullConsumerIndex")
    @Volatile private var fullConsumerIndex: Long = 0

    protected fun lvFullConsumerIndex() = fullConsumerIndex
    protected fun soFullConsumerIndex(newValue: Long) = F_C_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL9Pad6<E : Any>(capacity: Int) : SpscAtomicArrayQueueFullConsumerIndexField6<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicSuspendFlag6<E : Any>(capacity: Int) : SpscAtomicArrayQueueL9Pad6<E>(capacity) {
    private val S_FLAG_UPDATER = AtomicIntegerFieldUpdater.newUpdater<SpscAtomicSuspendFlag6<*>>(SpscAtomicSuspendFlag6::class.java, "suspendFlag")
    @Volatile private var suspendFlag: Int = NO_SUSPEND

    protected fun lvSuspendFlag() = suspendFlag
    protected fun loGetAndSetSuspendFlag(newValue: Int) = S_FLAG_UPDATER.getAndSet(this, newValue)
    protected fun soSuspendFlag(newValue: Int) { S_FLAG_UPDATER.set(this, newValue) }
}

abstract class SpscAtomicArrayQueueL10Pad6<E : Any>(capacity: Int) : SpscAtomicSuspendFlag6<E>(capacity) {
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
