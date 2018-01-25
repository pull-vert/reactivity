package channel.spsc4

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
import channel.Element
import channel.Sink
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
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
public open class SpScChannel4<E : Any>(
        /**
         * Buffer capacity.
         */
        capacity: Int
) : SpscAtomicArrayQueueL8Pad4<Element<E>>(capacity), Sink<E> {

    private fun tryResumeReceiveFast() {
        if (1 == lvEmptyPostFlag()) { // Consumer is suspended
//            if (FULL_SUSPEND == loGetAndSetPreSuspendFlag(NO_SUSPEND)) return // already resuming
            soEmptyPostFlag(0)
            val empty = lvEmpty()
//            println("FAST : Consumer is suspended, resume")
            soPreSuspendFlag(0)
            empty.resume(Unit)
        }
    }

    /**
     * Called when Consumer is or will be suspended
     */
    private fun tryResumeReceiveSlow() {
//        println("resumeReceive : Consumer is or will be suspended")
        do { } while (!compareAndSetEmptyPostFlag(1, 0)) // wait until EmptyPostFlag = 1
    // Consumer is suspended
//                println("SLOW : Consumer is suspended, resume")
        val empty = lvEmpty()
        empty.resume(Unit)
    }

    private fun handleEmptyOnClose() {
//        println("handleEmptyOnClose")
        // get current PreSuspendFlag
        if (EMPTY_SUSPEND == loGetAndSetPreSuspendFlag(NO_SUSPEND)) { // Consumer is or will be suspended
//            println("handleEmptyOnClose Consumer is or will be suspended")
            while (true) {
                if (1 == lvEmptyPostFlag()) { // Consumer is suspended
//                    println("handleEmptyOnClose Consumer is suspended")
                    // LoadLoad
                    val empty = lvEmpty()
                    empty.resume(Unit)
                    return
                }
            }
        }
    }

    private fun tryResumeSendFast() {
        if (1 == lvFullPostFlag()) { // Producer is suspended
//             println("tryResumeSend : Producer is suspended")
            soFullPostFlag(0)
            val full = lvFull()
            full.resume(Unit)
        }
    }

    /**
     * Called when Producer is or will be suspended
     */
    private fun tryResumeSendSlow() {
//        println("resumeSend : Producer is or will be suspended")
        while (true) {
//            if (NO_SUSPEND == lvPreSuspendFlag()) return // already resuming
            if (1 == lvFullPostFlag()) { // Producer is suspended
//                println("resumeSend : Producer is suspended")
                // LoadLoad
                soFullPostFlag(0)
                val full = lvFull()
                full.resume(Unit)
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
    fun offer(item: E): Boolean {
        // local load of field to avoid repeated loads after volatile reads
        val buffer = this.buffer
        val mask = this.mask
        val producerIndex = lvProducerIndex()
        val offset = calcElementOffset(producerIndex, mask)
        // StoreStore
        soElement(buffer, offset, Element(item))
        // ordered store -> atomic and ordered for size()
        soLazyProducerIndex(producerIndex + 1)
        // check if buffer is full
        if (producerIndex >= producerLimit && !hasRoomLeft(buffer, mask, producerIndex)) {
            return false
        }
        // handle empty case (= suspended Consumer)
//        tryResumeReceiveFast()
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
        if (EMPTY_SUSPEND == loGetAndSetPreSuspendFlag(FULL_SUSPEND)) tryResumeReceiveSlow() // notify Producer will Suspend
        // slow-path does suspend
        sendSuspend()
    }

    private suspend fun sendSuspend(): Unit = suspendCoroutine { cont ->
        soFull(cont)
        soLazyFullPostFlag(1) // lazy so we are sure coroutine is suspended when applies
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
        handleEmptyOnClose()
//        println("end of close")
    }

    private fun poll(): E? {
        // local load of field to avoid repeated loads after volatile reads
        val buffer = this.buffer
        val consumerIndex = lvConsumerIndex()
        val offset = calcElementOffset(consumerIndex)
        // LoadLoad
        val value = lvElement(buffer, offset)
        if (null == value) return null // empty buffer
        // StoreStore
        soElement(buffer, offset, null)
        // before suspend, check if Closed
        val closed = value.closeCause
        if (null != closed) {
            throw closed
        }
        // ordered store -> atomic and ordered for size()
        soLazyConsumerIndex(consumerIndex + 1)
        // we consumed the value from buffer, now check if Producer is full
//        tryResumeSendFast()
//      println("receive ${value.item}")
        return value.item as E // if producer was full
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
        if (FULL_SUSPEND == loGetAndSetPreSuspendFlag(EMPTY_SUSPEND)) tryResumeSendSlow() // notify Consumer will Suspend
        receiveSuspend()
        return receive() // re-call receive after suspension
    }

    private suspend fun receiveSuspend(): Unit = suspendCoroutine { cont ->
        // StoreStore
        soEmpty(cont)
        soLazyEmptyPostFlag(1)// lazy so we are sure coroutine is suspended when applies, test without !
//        println("receiveSuspend : Consumer suspend")
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

private const val NO_SUSPEND = 0
private const val FULL_SUSPEND = 1
private const val EMPTY_SUSPEND = -1

abstract class AtomicReferenceArrayQueue4<E : Any>(capacity: Int) {
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
        protected fun <E : Any> soElement(buffer: AtomicReferenceArray<E?>, offset: Int, value: E?) { buffer.lazySet(offset, value) }

        @JvmStatic
        protected fun calcElementOffset(index: Long, mask: Int) = index.toInt() and mask
    }
}

abstract class SpscAtomicArrayQueueColdField4<E : Any>(capacity: Int) : AtomicReferenceArrayQueue4<E>(capacity) {
    @JvmField protected val lookAheadStep: Int

    init {
        lookAheadStep = min(capacity / 4, MAX_LOOK_AHEAD_STEP)
    }

    companion object {
        val MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096)
    }
}

abstract class SpscAtomicArrayQueueL1Pad4<E : Any>(capacity: Int) : SpscAtomicArrayQueueColdField4<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueProducerIndexFields4<E : Any>(capacity: Int) : SpscAtomicArrayQueueL1Pad4<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val P_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueProducerIndexFields4<*>>(SpscAtomicArrayQueueProducerIndexFields4::class.java, "producerIndex")
    @Volatile private var producerIndex: Long = 0L
    @JvmField protected var producerLimit: Long = 0L

    protected fun lvProducerIndex() = producerIndex
    protected fun soLazyProducerIndex(newValue: Long) { P_INDEX_UPDATER.lazySet(this, newValue) }
}

abstract class SpscAtomicArrayQueueL2Pad4<E : Any>(capacity: Int) : SpscAtomicArrayQueueProducerIndexFields4<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicArrayQueueConsumerIndexField4<E : Any>(capacity: Int) : SpscAtomicArrayQueueL2Pad4<E>(capacity) {
    // TODO test with LongAdder with jdk8 !
    private val C_INDEX_UPDATER  = AtomicLongFieldUpdater.newUpdater<SpscAtomicArrayQueueConsumerIndexField4<*>>(SpscAtomicArrayQueueConsumerIndexField4::class.java, "consumerIndex")
    @Volatile private var consumerIndex: Long = 0L

    protected fun lvConsumerIndex() = consumerIndex
    protected fun soLazyConsumerIndex(newValue: Long) { C_INDEX_UPDATER.lazySet(this, newValue) }
}

abstract class SpscAtomicArrayQueueL3Pad4<E : Any>(capacity: Int) : SpscAtomicArrayQueueConsumerIndexField4<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class AtomicReferenceEmptyField4<E : Any>(capacity: Int) : SpscAtomicArrayQueueL3Pad4<E>(capacity) {
    private val EMPTY_UPDATER = AtomicReferenceFieldUpdater.newUpdater<AtomicReferenceEmptyField4<*>, Continuation<*>>(AtomicReferenceEmptyField4::class.java,
            Continuation::class.java, "empty")
    @Volatile private lateinit var empty: Continuation<Unit>

    internal fun lvEmpty() = empty
    internal fun soEmpty(value: Continuation<Unit>) { EMPTY_UPDATER.set(this, value) }
}

abstract class SpscAtomicArrayQueueL4Pad4<E : Any>(capacity: Int) : AtomicReferenceEmptyField4<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicPreSuspendFlag4<E : Any>(capacity: Int) : SpscAtomicArrayQueueL4Pad4<E>(capacity) {
    private val P_S_FLAG_UPDATER = AtomicIntegerFieldUpdater.newUpdater<SpscAtomicPreSuspendFlag4<*>>(SpscAtomicPreSuspendFlag4::class.java, "preSuspendFlag")
    @Volatile private var preSuspendFlag: Int = NO_SUSPEND

    protected fun lvPreSuspendFlag() = preSuspendFlag
    protected fun loGetAndSetPreSuspendFlag(newValue: Int) = P_S_FLAG_UPDATER.getAndSet(this, newValue)
    protected fun soPreSuspendFlag(newValue: Int) { P_S_FLAG_UPDATER.set(this, newValue) }
}

abstract class SpscAtomicArrayQueueL5Pad4<E : Any>(capacity: Int) : SpscAtomicPreSuspendFlag4<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class SpscAtomicEmptyPostFlag4<E : Any>(capacity: Int) : SpscAtomicArrayQueueL5Pad4<E>(capacity) {
    private val E_POST_FLAG_UPDATER = AtomicIntegerFieldUpdater.newUpdater<SpscAtomicEmptyPostFlag4<*>>(SpscAtomicEmptyPostFlag4::class.java, "emptyPostFlag")
    @Volatile private var emptyPostFlag: Int = 0

    protected fun lvEmptyPostFlag() = emptyPostFlag
    protected fun compareAndSetEmptyPostFlag(expect: Int, update: Int): Boolean = E_POST_FLAG_UPDATER.compareAndSet(this, expect, update)
    protected fun soLazyEmptyPostFlag(newValue: Int) { E_POST_FLAG_UPDATER.set(this, newValue) }
    protected fun soEmptyPostFlag(newValue: Int) { E_POST_FLAG_UPDATER.set(this, newValue) } // todo test with only lazySet
}

abstract class SpscAtomicArrayQueueL6Pad4<E : Any>(capacity: Int) : SpscAtomicEmptyPostFlag4<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

abstract class AtomicReferenceFullField4<E : Any>(capacity: Int) : SpscAtomicArrayQueueL6Pad4<E>(capacity) {
    private val FULL_UPDATER = AtomicReferenceFieldUpdater.newUpdater<AtomicReferenceFullField4<*>, Continuation<*>>(AtomicReferenceFullField4::class.java,
            Continuation::class.java, "full")
    @Volatile private lateinit var full: Continuation<Unit>

    internal fun lvFull() = full
    internal fun soFull(value: Continuation<Unit>) { FULL_UPDATER.set(this, value) }
}

abstract class SpscAtomicArrayQueueL7Pad4<E : Any>(capacity: Int) : AtomicReferenceFullField4<E>(capacity) {
    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L

    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
}

//abstract class SpscAtomicFullPreFlag4<E : Any>(capacity: Int) : SpscAtomicArrayQueueL7Pad4<E>(capacity) {
//    private val F_PRE_FLAG_UPDATER = AtomicIntegerFieldUpdater.newUpdater<SpscAtomicFullPreFlag4<*>>(SpscAtomicFullPreFlag4::class.java, "fullPreFlag")
//    @Volatile private var fullPreFlag: Int = 0
//
//    protected fun lvFullPreFlag() = fullPreFlag
//    protected fun loGetAndSetFullPreFlag(newValue: Int) = F_PRE_FLAG_UPDATER.getAndSet(this, newValue)
//    protected fun soFullPreFlag(newValue: Int) = F_PRE_FLAG_UPDATER.set(this, newValue)
//}
//
//abstract class SpscAtomicArrayQueueL8Pad4<E : Any>(capacity: Int) : SpscAtomicFullPreFlag4<E>(capacity) {
//    private val p01: Long = 0L;private val p02: Long = 0L;private val p03: Long = 0L;private val p04: Long = 0L;private val p05: Long = 0L;private val p06: Long = 0L;private val p07: Long = 0L
//
//    private val p10: Long = 0L;private val p11: Long = 0L;private val p12: Long = 0L;private val p13: Long = 0L;private val p14: Long = 0L;private val p15: Long = 0L;private val p16: Long = 0L;private val p17: Long = 0L
//}

abstract class SpscAtomicFullPostFlag4<E : Any>(capacity: Int) : SpscAtomicArrayQueueL7Pad4<E>(capacity) {
    private val F_POST_FLAG_UPDATER = AtomicIntegerFieldUpdater.newUpdater<SpscAtomicFullPostFlag4<*>>(SpscAtomicFullPostFlag4::class.java, "fullPostFlag")
    @Volatile private var fullPostFlag: Int = 0

    protected fun lvFullPostFlag() = fullPostFlag
    protected fun soLazyFullPostFlag(newValue: Int) { F_POST_FLAG_UPDATER.lazySet(this, newValue) }
    protected fun soFullPostFlag(newValue: Int) { F_POST_FLAG_UPDATER.set(this, newValue) }
}

abstract class SpscAtomicArrayQueueL8Pad4<E : Any>(capacity: Int) : SpscAtomicFullPostFlag4<E>(capacity) {
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
