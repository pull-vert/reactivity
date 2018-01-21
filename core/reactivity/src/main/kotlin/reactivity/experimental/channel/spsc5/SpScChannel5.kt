package reactivity.experimental.channel.spsc5

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
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import reactivity.experimental.channel.Element
import reactivity.experimental.channel.Sink
import java.util.concurrent.atomic.AtomicLongFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
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
public open class SpScChannel5<E : Any>(
        /**
         * Buffer capacity.
         */
        capacity: Int
) : SpscAtomicArrayQueueL11Pad5<Element<E>>(capacity), Sink<E> {

    private fun handleEmpty() {
        val emptyConsumerIndex = lvEmptyConsumerIndex()
        // LoadLoad
        val empty = lvEmpty()
        if (emptyConsumerIndex == empty.index) {
//            println("handleEmpty Consumer is suspended")
            soEmptyIndex(0L)
            empty.resume()
            soEmptyConsumerIndex(emptyConsumerIndex + 1)
        }
    }

    private fun handleEmptyStrict() {
        // Ordered get the current emptyIndex
        val emptyIndex = loGetAndSetEmptyIndex(0L)
        if (1L == emptyIndex) {// then we know Consumer is suspended !
            val emptyConsumerIndex = lvEmptyConsumerIndex()
//            println("handleEmptyStrict Consumer is suspended")
            while(true) {
                // LoadLoad
                val empty = lvEmpty()
                if (emptyConsumerIndex == empty.index) {
//                    println("handleEmptyStrict empty=$empty")
                    // we have the Empty suspended
                    empty.resume()
                    soEmptyConsumerIndex(emptyConsumerIndex + 1)
                    return
                }
            }
        }
    }

    private fun handleFull() {
        val fullConsumerIndex = lvFullConsumerIndex()
        // LoadLoad
        val full = lvFull()
        if (fullConsumerIndex == full.index) {
//            println("handleFull Producer is suspended")
            soFullIndex(0L)
            full.resume()
            soFullConsumerIndex(fullConsumerIndex + 1)
        }
    }

    private fun handleFullStrict() {
        // Ordered get the current fullIndex
        val fullIndex = loGetAndSetFullIndex(0L)
        if (1L == fullIndex) { // then we know Producer is suspended !
            val fullConsumerIndex = lvFullConsumerIndex()
//            println("handleFullStrict Producer is suspended")
            while(true) {
                // LoadLoad
                val full = lvFull()
                if (fullConsumerIndex == full.index) {
//                    println("handleFullStrict full=$full")
                    // we have the Full suspended
                    full.resume()
                    soFullConsumerIndex(fullConsumerIndex + 1)
                    return
                }
            }
        }
    }

    /**
     * Offer the value in buffer
     * Return true if there is room left in buffer, false if just 1 spot left in the buffer
     * <p>
     * This implementation is correct for single producer thread use only.
     */
    fun offer(item: E): Boolean {
        // local load of field to avoid repeated loads after volatile reads
        val buffer = this.buffer
        val mask = this.mask
        val producerIndex = lvProducerIndex()
        val offset = calcElementOffset(producerIndex, mask)
        // StoreStore
        soElement(buffer, offset, Element(item))
        // ordered store -> atomic and ordered for size()
        soProducerIndex(producerIndex + 1)
        // check if buffer is full
        if (producerIndex >= producerLimit && !hasRoomLeft(buffer, mask, producerIndex)) {
            return false
        }
        // handle empty case (= suspended Consumer)
        handleEmpty()
        return true
    }

    private fun hasRoomLeft(buffer: AtomicReferenceArray<Element<E>?>, mask: Int, producerIndex: Long): Boolean {
        val lookAheadStep = this.lookAheadStep
        if (null == lvElement(buffer, calcElementOffset(producerIndex + lookAheadStep, mask))) {
            // LoadLoad
            producerLimit = producerIndex + lookAheadStep
        } else {
            val offsetNext = calcElementOffset(producerIndex + 2, mask) // always leave one free spot for Closed
            return null == lvElement(buffer, offsetNext)
        }
        return true
    }

    final override suspend fun send(item: E) {
        // fast path -- try offer non-blocking
        if (offer(item)) return
        soFullIndex(1L) // notify Producer will Suspend
        // slow-path does suspend
        val fullProducerIndex = lvFullProducerIndex()
        soFullProducerIndex(fullProducerIndex + 1)
        sendSuspend(fullProducerIndex)
    }

    private suspend fun sendSuspend(fullProducerIndex: Long): Unit = suspendCoroutine { cont ->
        soFull(Suspended(cont, fullProducerIndex))
        // Must handle empty case strict to avoid both Producer and Consumer are suspended
        handleEmptyStrict()
        //        cont.invokeOnCompletion { // todo test without first and then try it with a Unit test that Cancels
//            soFull(null)
//        }
    }

    /**
     * <p>
     * This implementation is correct for single producer thread use only.
     */
    override fun close(cause: Throwable?) {
//        println("close")
        val buffer = this.buffer
        val mask = this.mask
        val producerIndex = lvProducerIndex()
        val offset = calcElementOffset(producerIndex, mask)
        // StoreStore
        val closeCause = cause ?: ClosedReceiveChannelException(DEFAULT_CLOSE_MESSAGE)
        soElement(buffer, offset, Element(closeCause = closeCause))
        // handle empty case (= suspended Consumer)
        handleEmptyStrict()
    }

    /**
     * <p>
     * This implementation is correct for single producer thread use only.
     */
    suspend fun receive(): E {
        // local load of field to avoid repeated loads after volatile reads
        val buffer = this.buffer
        val consumerIndex = lvConsumerIndex()
        val offset = calcElementOffset(consumerIndex)
        // LoadLoad
        val value = lvElement(buffer, offset)
        if (null == value) { // empty buffer
            soEmptyIndex(1L) // notify Consumer will Suspend
            val emptyProducerIndex = lvEmptyProducerIndex()
            soEmptyProducerIndex(emptyProducerIndex + 1)
            receiveSuspend(emptyProducerIndex)
            return receive() // re-call receive after suspension
        } else {
            // StoreStore
            soElement(buffer, offset, null)
            // before suspend, check if Closed
            val closed = value.closeCause
            if (null != closed) {
                throw closed
            }
            // ordered store -> atomic and ordered for size()
            soConsumerIndex(consumerIndex + 1)
            // we consumed the value from buffer, now check if Producer is full
            handleFull()
//            println("receive ${value.item}")
            return value.item as E // if producer was full
        }
    }

    private suspend fun receiveSuspend(emptyProducerIndex: Long): Unit = suspendCoroutine { cont ->
        // StoreStore
        soEmpty(Suspended(cont, emptyProducerIndex))
        // Must handle full case strict to avoid both Producer and Consumer are suspended
        handleFullStrict()
//        println("receiveSuspend $emptyOffset")
        //        cont.invokeOnCompletion { // todo test without first and then try it with a Unit test that Cancels parent
//            _empty.lazySet(null)
//        }
    }
}

object TOKEN: Continuation<Unit> {
    override val context: CoroutineContext
        get() = throw UnsupportedOperationException()

    override fun resume(value: Unit) {
        throw UnsupportedOperationException()
    }

    override fun resumeWithException(exception: Throwable) {
        throw UnsupportedOperationException()
    }
}

private const val DEFAULT_CLOSE_MESSAGE = "SpScChannel was closed"

abstract class AtomicReferenceArrayQueue5<E : Any>(capacity: Int) {
    @JvmField protected val buffer = AtomicReferenceArray<E?>(capacity)
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

    /**
     * Element to store when suspend Producer or Consumer
     */
    protected data class Suspended(
            private var cont: Continuation<Unit>,
            internal var index: Long
    ) {
        fun resume() { cont.resume(Unit) }
        override fun toString() = "Suspended[index=$index,cont=$cont]"
    }
}

abstract class SpscAtomicArrayQueueColdField5<E : Any>(capacity: Int) : AtomicReferenceArrayQueue5<E>(capacity) {
    @JvmField protected val lookAheadStep: Int

    init {
        lookAheadStep = min(capacity / 4, MAX_LOOK_AHEAD_STEP)
    }

    companion object {
        val MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096)
    }
}

abstract class SpscAtomicArrayQueueL1Pad5<E : Any>(capacity: Int) : SpscAtomicArrayQueueColdField5<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueProducerIndexFields5<E : Any>(capacity: Int) : SpscAtomicArrayQueueL1Pad5<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val P_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueProducerIndexFields5<*>>(SpscAtomicArrayQueueProducerIndexFields5::class.java, "producerIndex")
    @Volatile private var producerIndex: Long = 0L
    @JvmField protected var producerLimit: Long = 0L

    protected fun lvProducerIndex() = producerIndex
    protected fun soProducerIndex(newValue: Long) = P_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL2Pad5<E : Any>(capacity: Int) : SpscAtomicArrayQueueProducerIndexFields5<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueConsumerIndexField5<E : Any>(capacity: Int) : SpscAtomicArrayQueueL2Pad5<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val C_INDEX_UPDATER  = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueConsumerIndexField5<*>>(SpscAtomicArrayQueueConsumerIndexField5::class.java, "consumerIndex")
    @Volatile private var consumerIndex: Long = 0L

    protected fun lvConsumerIndex() = consumerIndex
    protected fun soConsumerIndex(newValue: Long) = C_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL3Pad5<E : Any>(capacity: Int) : SpscAtomicArrayQueueConsumerIndexField5<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class AtomicReferenceEmptyField5<E : Any>(capacity: Int) : SpscAtomicArrayQueueL3Pad5<E>(capacity) {
    private val EMPTY_UPDATER = AtomicReferenceFieldUpdater.newUpdater<AtomicReferenceEmptyField5<*>, Suspended>(AtomicReferenceEmptyField5::class.java,
            Suspended::class.java, "empty")
    @Volatile private var empty = Suspended(TOKEN, -1)

    protected fun lvEmpty() = empty
    protected fun soEmpty(value: Suspended?) = EMPTY_UPDATER.lazySet(this, value) // todo check if must need a set instead of lazySet
}

abstract class SpscAtomicArrayQueueL4Pad5<E : Any>(capacity: Int) : AtomicReferenceEmptyField5<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueEmpty4IndexField5<E : Any>(capacity: Int) : SpscAtomicArrayQueueL4Pad5<E>(capacity) {
    private val E_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueEmpty4IndexField5<*>>(SpscAtomicArrayQueueEmpty4IndexField5::class.java, "emptyIndex")
    @Volatile private var emptyIndex: Long = 0L

    protected fun loGetAndSetEmptyIndex(newValue: Long) = E_INDEX_UPDATER.getAndSet(this, newValue)
    protected fun soEmptyIndex(newValue: Long) = E_INDEX_UPDATER.set(this, newValue)
}

abstract class SpscAtomicArrayQueueL5Pad5<E : Any>(capacity: Int) : SpscAtomicArrayQueueEmpty4IndexField5<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueEmptyProducerIndexField5<E : Any>(capacity: Int) : SpscAtomicArrayQueueL5Pad5<E>(capacity) {
    private val E_P_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueEmptyProducerIndexField5<*>>(SpscAtomicArrayQueueEmptyProducerIndexField5::class.java, "emptyProducerIndex")
    @Volatile private var emptyProducerIndex: Long = 0L

    protected fun lvEmptyProducerIndex() = emptyProducerIndex
    protected fun soEmptyProducerIndex(newValue: Long) = E_P_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL6Pad5<E : Any>(capacity: Int) : SpscAtomicArrayQueueEmptyProducerIndexField5<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueEmptyConsumerIndexField5<E : Any>(capacity: Int) : SpscAtomicArrayQueueL6Pad5<E>(capacity) {
    private val E_C_INDEX_UPDATER  = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueEmptyConsumerIndexField5<*>>(SpscAtomicArrayQueueEmptyConsumerIndexField5::class.java, "emptyConsumerIndex")
    @Volatile private var emptyConsumerIndex: Long = 0L

    protected fun lvEmptyConsumerIndex() = emptyConsumerIndex
    protected fun soEmptyConsumerIndex(newValue: Long) = E_C_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL7Pad5<E : Any>(capacity: Int) : SpscAtomicArrayQueueEmptyConsumerIndexField5<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class AtomicReferenceFullField5<E : Any>(capacity: Int) : SpscAtomicArrayQueueL7Pad5<E>(capacity) {
    private val FULL_UPDATER = AtomicReferenceFieldUpdater.newUpdater<AtomicReferenceFullField5<*>, Suspended>(AtomicReferenceFullField5::class.java,
            Suspended::class.java, "full")
    @Volatile private var full = Suspended(TOKEN, -1)

    protected fun lvFull() = full
    protected fun soFull(value: Suspended?) = FULL_UPDATER.lazySet(this, value) // todo check if must need a set instead of lazySet
}

abstract class SpscAtomicArrayQueueL8Pad5<E : Any>(capacity: Int) : AtomicReferenceFullField5<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueFullIndexField5<E : Any>(capacity: Int) : SpscAtomicArrayQueueL8Pad5<E>(capacity) {
    private val F_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueFullIndexField5<*>>(SpscAtomicArrayQueueFullIndexField5::class.java, "fullIndex")
    @Volatile private var fullIndex: Long = 0L

    protected fun loGetAndSetFullIndex(newValue: Long) = F_INDEX_UPDATER.getAndSet(this, newValue)
    protected fun soFullIndex(newValue: Long) = F_INDEX_UPDATER.set(this, newValue)
}

abstract class SpscAtomicArrayQueueL9Pad5<E : Any>(capacity: Int) : SpscAtomicArrayQueueFullIndexField5<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueFullProducerIndexField5<E : Any>(capacity: Int) : SpscAtomicArrayQueueL9Pad5<E>(capacity) {
    private val F_P_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueFullProducerIndexField5<*>>(SpscAtomicArrayQueueFullProducerIndexField5::class.java, "fullProducerIndex")
    @Volatile private var fullProducerIndex: Long = 0L

    protected fun lvFullProducerIndex() = fullProducerIndex
    protected fun soFullProducerIndex(newValue: Long) = F_P_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL10Pad5<E : Any>(capacity: Int) : SpscAtomicArrayQueueFullProducerIndexField5<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueFullConsumerIndexField5<E : Any>(capacity: Int) : SpscAtomicArrayQueueL10Pad5<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val F_C_INDEX_UPDATER  = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueFullConsumerIndexField5<*>>(SpscAtomicArrayQueueFullConsumerIndexField5::class.java, "fullConsumerIndex")
    @Volatile private var fullConsumerIndex: Long = 0

    protected fun lvFullConsumerIndex() = fullConsumerIndex
    protected fun soFullConsumerIndex(newValue: Long) = F_C_INDEX_UPDATER.lazySet(this, newValue)
}

abstract class SpscAtomicArrayQueueL11Pad5<E : Any>(capacity: Int) : SpscAtomicArrayQueueFullConsumerIndexField5<E>(capacity) {
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
