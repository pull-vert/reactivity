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

package reactivity.experimental.intrinsics

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.loop
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
 * @param <E>
 * @author nitsanw, adapted by fmo
 */
class LockFreeSPSCQueue<E>(capacity: Int) : SpscAtomicArrayQueueL3Pad<E>(capacity) {

    /**
     * This implementation is correct for single producer thread use only.
     * Return the content of buffer at producerIndex.
     */
    fun offer(e: Any): Any? {
        // local load of field to avoid repeated loads after volatile reads
        val buffer = this.buffer
        val mask = this.mask
        _producerIndex.loop { producerIndex ->
            if (producerIndex >= producerLimit) {
                val f = offerSlowPath(buffer, mask, producerIndex)
                if (null != f) return f // buffer is full or closed for send
            }
            val offset = calcElementOffset(producerIndex, mask)
            val f = lvElement(buffer, offset)
            if (null != f) return f // buffer is closed for send
            // StoreStore
            soElement(buffer, offset, e)
            // ordered store -> atomic and ordered for size()
            soProducerIndex(producerIndex + 1)
            return f
        }
    }

    private fun offerSlowPath(buffer: AtomicReferenceArray<Any?>, mask: Int, producerIndex: Long): Any? {
        val lookAheadStep = this.lookAheadStep
        if (null == lvElement(buffer, calcElementOffset(producerIndex + lookAheadStep, mask))) {
            // LoadLoad
            producerLimit = producerIndex + lookAheadStep
        } else {
            val offset = calcElementOffset(producerIndex, mask)
            return lvElement(buffer, offset)
        }
        return null
    }

    /**
     * This implementation is correct for single consumer thread use only.
     */
    fun poll(): Any? {
        _consumerIndex.loop { consumerIndex ->
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
    }

    fun nextValueToConsume(): Any? {
        _consumerIndex.loop { consumerIndex ->
            val offset = calcElementOffset(consumerIndex + 1)
            val buffer = this.buffer
            return lvElement(buffer, offset)
        }
    }

    fun prevValueToConsume(): Any? {
        _consumerIndex.loop { consumerIndex ->
            val offset = calcElementOffset(consumerIndex - 1)
            val buffer = this.buffer
            return lvElement(buffer, offset)
        }
    }

    inline fun modifyNextValueToConsumeIfPrev(new: Any, old: Any?, predicate: (Any?) -> Boolean): Boolean {
        val prev = prevValueToConsume()
        if (!predicate(prev)) return false
        return modifyNextValueToConsume(new, old)
    }

    fun modifyNextValueToConsume(new: Any, old: Any?): Boolean {
        _consumerIndex.loop { consumerIndex ->
            val offset = calcElementOffset(consumerIndex + 1)
            val buffer = this.buffer
            return buffer.compareAndSet(offset, old, new)

        }
    }
}

abstract class AtomicReferenceArrayQueue<E>(capacity: Int) {
    @JvmField protected val buffer = AtomicReferenceArray<Any?>(capacity)
    @JvmField protected val mask: Int = capacity - 1

    init {
        check(capacity > 0) { "capacity must be positive" }
        check(capacity and mask == 0) { "capacity must be a power of 2" }
    }

    protected fun calcElementOffset(index: Long) = index.toInt() and mask

    protected companion object {
        @JvmStatic
        protected fun lvElement(buffer: AtomicReferenceArray<Any?>, offset: Int) = buffer[offset]

        @JvmStatic
        protected fun soElement(buffer: AtomicReferenceArray<Any?>, offset: Int, value: Any?) = buffer.lazySet(offset, value)

        @JvmStatic
        protected fun calcElementOffset(index: Long, mask: Int) = index.toInt() and mask
    }
}

abstract class SpscAtomicArrayQueueColdField<E>(capacity: Int) : AtomicReferenceArrayQueue<E>(capacity) {
    @JvmField protected val lookAheadStep: Int

    init {
        lookAheadStep = min(capacity / 4, MAX_LOOK_AHEAD_STEP)
    }

    companion object {
        val MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096)
    }
}

abstract class SpscAtomicArrayQueueL1Pad<E>(capacity: Int) : SpscAtomicArrayQueueColdField<E>(capacity) {
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

abstract class SpscAtomicArrayQueueProducerIndexFields<E>(capacity: Int) : SpscAtomicArrayQueueL1Pad<E>(capacity) {
    protected val _producerIndex = atomic(0L)
    @JvmField protected var producerLimit: Long = 0L

    protected fun soProducerIndex(newValue: Long) = _producerIndex.lazySet(newValue)
}

abstract class SpscAtomicArrayQueueL2Pad<E>(capacity: Int) : SpscAtomicArrayQueueProducerIndexFields<E>(capacity) {
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

abstract class SpscAtomicArrayQueueConsumerIndexField<E>(capacity: Int) : SpscAtomicArrayQueueL2Pad<E>(capacity) {
    protected val _consumerIndex = atomic(0L)

    protected fun soConsumerIndex(newValue: Long) = _consumerIndex.lazySet(newValue)
}

abstract class SpscAtomicArrayQueueL3Pad<E>(capacity: Int) : SpscAtomicArrayQueueConsumerIndexField<E>(capacity) {
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
