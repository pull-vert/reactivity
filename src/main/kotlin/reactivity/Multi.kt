package reactivity

import kotlinx.coroutines.experimental.launch
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactivity.internal.suspending.SuspendingIterator
import reactivity.internal.suspending.SuspendingSequence
import reactivity.internal.suspending.SuspendingSequenceBuilder
import reactivity.internal.suspending.suspendingIterator
import reactivity.internal.util.getAndAddCap
import reactivity.internal.util.validate
import java.util.concurrent.atomic.AtomicLongFieldUpdater
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

/**
 * Created by frederic.montariol on 11/04/2017.
 */

fun <T> multiSuspendingSequence(
        context: CoroutineContext,
        block: suspend SuspendingSequenceBuilder<T>.() -> Unit
): Multi<T> = Multi(context, block)

class Multi<T> internal constructor(open val context: CoroutineContext,
                                    open val block: suspend SuspendingSequenceBuilder<T>.() -> Unit) : Publisher<T> {

    var producer: SuspendingSequence<T>

    init {
        producer = object : SuspendingSequence<T> {
            override fun iterator(): SuspendingIterator<T> = suspendingIterator(context, block)
        }
    }

    override fun subscribe(s: Subscriber<in T>) {
        val subscription = SuspendingSequenceSubscription(context, s, producer.iterator())
        s.onSubscribe(subscription)
    }

//    fun subscribe(nOnNext: ((T) -> Unit)? = null, nOnError: ((Throwable) -> Unit)? = null, nOnComplete: (() -> Unit)? = null) {
//        val sub = CallbackSubscriber(nOnNext, nOnError, nOnComplete)
//        subscribe(sub)
//    }

    // static constructors
    companion object {
        fun fromRange(start: Int, count: Int, context: CoroutineContext = EmptyCoroutineContext): Multi<Int> {
            return Multi(context) {
                for (x in start until start + count) {
                    yield(x)
                }
            }
        }
    }

    // Operators

    /**
     * Delay each element in the reactiveStream
     */
    fun delay(context: CoroutineContext = EmptyCoroutineContext, delay: Long) : Multi<T> {
        return Multi(context) {
            for (item in producer) {
                yield(item)
                kotlinx.coroutines.experimental.delay(delay)
            }
        }
    }

    private class SuspendingSequenceSubscription<T>(val context: CoroutineContext,
                                                    val actual: Subscriber<in T>,
                                                    var iterator: SuspendingIterator<T>) : Subscription /* Fuseable.QueueSubscription<T>*/ {

        @Volatile
        var cancelled: Boolean = false

        @Volatile
        var requested: Long = 0
        val REQUESTED: AtomicLongFieldUpdater<SuspendingSequenceSubscription<*>> = AtomicLongFieldUpdater.newUpdater<SuspendingSequenceSubscription<*>>(SuspendingSequenceSubscription::class.java,
                "requested")

        override fun cancel() {
            cancelled = true
        }

        /**
         * see [Subscription.request]
         *
         * In case of a canceled subscription, see [Subscription.cancel] : no onError, no onComplete
         */
        override fun request(n: Long) {
            if (validate(n)) {
                if (getAndAddCap(REQUESTED, this, n) == 0L) {
                    if (n == Long.MAX_VALUE) publisherPush()
                    else subscriberPull(n)
                }
            }
        }

        private fun subscriberPull(n: Long) {
            val a = iterator
            val s = actual
            // Starts a coroutine block for the SuspendingIterator
            launch(context) {
                var e = 0L

                while (true) {

                    while (e != n) {
                        if (cancelled) {
                            return@launch
                        }

                        val b: Boolean

                        try {
                            b = a.hasNext()
                        } catch (ex: Throwable) {
                            s.onError(ex)
                            return@launch
                        }

                        if (cancelled) {
                            return@launch
                        }

                        if (!b) {
                            s.onComplete()
                            return@launch
                        }

                        val t: T

                        try {
                            t = a.next()
                        } catch (ex: Throwable) {
                            s.onError(ex)
                            return@launch
                        }

                        if (cancelled) {
                            return@launch
                        }

                        s.onNext(t)

                        e++
                    }

                    var l = requested

                    if (l == e) {
                        l = REQUESTED.addAndGet(this@SuspendingSequenceSubscription, -e)
                        if (l == 0L) {
                            return@launch
                        }
                        e = 0L
                    }
                }
            }
        }

        private fun publisherPush() {
            val a = iterator
            val s = actual
            // Starts a coroutine block for the SuspendingSequence
            launch(context) {
                while (true) {


                    if (cancelled) {
                        return@launch
                    }

                    val b: Boolean

                    try {
                        b = a.hasNext()
                    } catch (ex: Throwable) {
                        s.onError(ex)
                        return@launch
                    }

                    if (cancelled) {
                        return@launch
                    }

                    if (!b) {
                        s.onComplete()
                        return@launch
                    }

                    val t: T

                    try {
                        t = a.next()
                    } catch (ex: Throwable) {
                        s.onError(ex)
                        return@launch
                    }

                    if (cancelled) {
                        return@launch
                    }

                    s.onNext(t)
                }
            }
        }
    }
}