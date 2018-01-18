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

package internal

import java.util.concurrent.atomic.AtomicLongFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.math.min


/**
 *
 * Lock-free Single-Producer Single-Consumer Queue backed by a pre-allocated buffer.
 * Based on Ring Buffer = circular array = circular buffer
 * http://psy-lob-saw.blogspot.fr/2014/04/notes-on-concurrent-ring-buffer-queue.html
 * http://psy-lob-saw.blogspot.fr/2014/01/picking-2013-spsc-queue-champion.html
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
class LockFreeSPSCQueue<E : Any>(capacity: Int) : SpscAtomicArrayQueueL3Pad<E>(capacity) {

    /**
     * This implementation is correct for single producer thread use only.
     * Return true if offer is done, false if buffer full
     */
    fun offer(e: E): Boolean {
        // local load of field to avoid repeated loads after volatile reads
        val buffer = this.buffer
        val mask = this.mask
        val producerIndex = producerIndex
        if (producerIndex >= producerLimit && !offerSlowPath(buffer, mask, producerIndex)) {
            return false // buffer is full
        }
        val offset = calcElementOffset(producerIndex, mask)
        // StoreStore
        soElement(buffer, offset, e)
        // ordered store -> atomic and ordered for size()
        soProducerIndex(producerIndex + 1)
        return true
    }

    private fun offerSlowPath(buffer: AtomicReferenceArray<E?>, mask: Int, producerIndex: Long): Boolean {
        val lookAheadStep = this.lookAheadStep
        if (null == lvElement(buffer, calcElementOffset(producerIndex + lookAheadStep, mask))) {
            // LoadLoad
            producerLimit = producerIndex + lookAheadStep
        } else {
            val offset = calcElementOffset(producerIndex, mask)
            return null == lvElement(buffer, offset)
        }
        return true
    }

    /**
     * This implementation is correct for single consumer thread use only.
     */
    fun poll(): E? {
        val consumerIndex = consumerIndex
        val offset = calcElementOffset(consumerIndex)
        // local load of field to avoid repeated loads after volatile reads
        val buffer = this.buffer
        // LoadLoad
        val e = lvElement(buffer, offset) ?: return null // if null, buffer is empty
        // StoreStore
        soElement(buffer, offset, null)
        // ordered store -> atomic and ordered for size()
        soConsumerIndex(consumerIndex + 1)
        return e
    }

//    fun nextValueToConsume(): Any? {
//        _consumerIndex.loop { consumerIndex ->
//            val offset = calcElementOffset(consumerIndex + 1)
//            val buffer = this.buffer
//            return lvElement(buffer, offset)
//        }
//    }
//
//    fun prevValueToConsume(): Any? {
//        _consumerIndex.loop { consumerIndex ->
//            val offset = calcElementOffset(consumerIndex - 1)
//            val buffer = this.buffer
//            return lvElement(buffer, offset)
//        }
//    }
//
//    inline fun modifyNextValueToConsumeIfPrev(new: Any, old: Any?, predicate: (Any?) -> Boolean): Boolean {
//        val prev = prevValueToConsume()
//        if (!predicate(prev)) return false
//        return modifyNextValueToConsume(new, old)
//    }
//
//    fun modifyNextValueToConsume(new: Any, old: Any?): Boolean {
//        _consumerIndex.loop { consumerIndex ->
//            val offset = calcElementOffset(consumerIndex + 1)
//            val buffer = this.buffer
//            return buffer.compareAndSet(offset, old, new)
//
//        }
//    }
//
//    fun close(closed: Any) {
//        val buffer = this.buffer
//        // StoreStore
//        soElement(buffer, capacity, closed)
//    }
//
//    fun getClosed(): Any? {
//        val buffer = this.buffer
//        return lvElement(buffer, capacity)
//    }
}

abstract class AtomicReferenceArrayQueue<E : Any>(capacity: Int) {
    @JvmField protected val buffer = AtomicReferenceArray<E?>(capacity + 1) // keep one slot for closed
    @JvmField protected val mask: Int = capacity - 1

    init {
        check(capacity > 0) { "capacity must be positive" }
        check(capacity and mask == 0) { "capacity must be a power of 2" }
    }

    protected fun calcElementOffset(index: Long) = index.toInt() and mask

    protected companion object {
        @JvmStatic
        protected fun <E : Any> lvElement(buffer: AtomicReferenceArray<E?>, offset: Int) = buffer.get(offset)

        @JvmStatic
        protected fun <E : Any> soElement(buffer: AtomicReferenceArray<E?>, offset: Int, value: E?) = buffer.lazySet(offset, value)

        @JvmStatic
        protected fun calcElementOffset(index: Long, mask: Int) = index.toInt() and mask
    }
}

abstract class SpscAtomicArrayQueueColdField<E : Any>(capacity: Int) : AtomicReferenceArrayQueue<E>(capacity) {
    @JvmField protected val lookAheadStep: Int

    init {
        lookAheadStep = min(capacity / 4, MAX_LOOK_AHEAD_STEP)
    }

    companion object {
        val MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096)
    }
}

abstract class SpscAtomicArrayQueueL1Pad<E : Any>(capacity: Int) : SpscAtomicArrayQueueColdField<E>(capacity) {
    private val p01: Long = 0
    private val p02: Long = 0
    private val p03: Long = 0
    private val p04: Long = 0
    private val p05: Long = 0
    private val p06: Long = 0
    private val p07: Long = 0

    private val p10: Long = 0
    private val p11: Long = 0
    private val p12: Long = 0
    private val p13: Long = 0
    private val p14: Long = 0
    private val p15: Long = 0
    private val p16: Long = 0
    private val p17: Long = 0
}

abstract class SpscAtomicArrayQueueProducerIndexFields<E : Any>(capacity: Int) : SpscAtomicArrayQueueL1Pad<E>(capacity) {
    private val P_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueProducerIndexFields<*>>(SpscAtomicArrayQueueProducerIndexFields::class.java, "producerIndex")
    @Volatile @JvmField protected var producerIndex: Long = 0
    @JvmField protected var producerLimit: Long = 0L

    protected fun soProducerIndex(newValue: Long) = P_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL2Pad<E : Any>(capacity: Int) : SpscAtomicArrayQueueProducerIndexFields<E>(capacity) {
    private val p01: Long = 0
    private val p02: Long = 0
    private val p03: Long = 0
    private val p04: Long = 0
    private val p05: Long = 0
    private val p06: Long = 0
    private val p07: Long = 0

    private val p10: Long = 0
    private val p11: Long = 0
    private val p12: Long = 0
    private val p13: Long = 0
    private val p14: Long = 0
    private val p15: Long = 0
    private val p16: Long = 0
    private val p17: Long = 0
}

abstract class SpscAtomicArrayQueueConsumerIndexField<E : Any>(capacity: Int) : SpscAtomicArrayQueueL2Pad<E>(capacity) {
    private val C_INDEX_UPDATER  = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueConsumerIndexField<*>>(SpscAtomicArrayQueueConsumerIndexField::class.java, "consumerIndex")
    @Volatile @JvmField protected var consumerIndex: Long = 0

    protected fun soConsumerIndex(newValue: Long) = C_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL3Pad<E : Any>(capacity: Int) : SpscAtomicArrayQueueConsumerIndexField<E>(capacity) {
    private val p01: Long = 0
    private val p02: Long = 0
    private val p03: Long = 0
    private val p04: Long = 0
    private val p05: Long = 0
    private val p06: Long = 0
    private val p07: Long = 0

    private val p10: Long = 0
    private val p11: Long = 0
    private val p12: Long = 0
    private val p13: Long = 0
    private val p14: Long = 0
    private val p15: Long = 0
    private val p16: Long = 0
    private val p17: Long = 0
}
