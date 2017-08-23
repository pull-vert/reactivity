package io.http2.koala.internal.suspending

import kotlinx.coroutines.experimental.launch
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.CoreSubscriber
import reactor.core.publisher.Flux
import kotlin.coroutines.experimental.CoroutineContext

fun <T> fluxSuspendingSequence(
        context: CoroutineContext,
        block: suspend SuspendingSequenceBuilder<T>.() -> Unit
): Flux<T> = FluxSuspendingSequence(context, block)

internal class FluxSuspendingSequence<T> internal constructor(open val context: CoroutineContext,
                                                              open val block: suspend SuspendingSequenceBuilder<T>.() -> Unit) : Flux<T>() {
    var producer: SuspendingSequence<T>

    init {
        producer = object : SuspendingSequence<T> {
            override fun iterator(): SuspendingIterator<T> = suspendingIterator(context, block)
        }
    }

    override fun subscribe(actual: CoreSubscriber<in T>?) {
        requireNotNull(actual) { "The Subscriber must not be null " }
        val subscription = SuspendingSequenceSubscription(context, actual!!, producer)
        actual.onSubscribe(subscription)
    }

    private class SuspendingSequenceSubscription<T>(val context: CoroutineContext,
                                                    val actual: CoreSubscriber<in T>,
                                                    var sequence: SuspendingSequence<T>) : Subscription /* Fuseable.QueueSubscription<T>*/ {

        @Volatile
        var cancelled: Boolean = false

        override fun cancel() {
            cancelled = true
        }

        /**
         * see [Subscription.request]
         *
         * In case of a canceled subscription, see [Subscription.cancel] : no onError, no onComplete
         */
        override fun request(n: Long) {
            if (n <= 0) actual.onError(IllegalArgumentException("n parameter must be greater than 0"))

            if (Long.MAX_VALUE == n) publisherPush()
            else subscriberPull(n)
        }

        private fun subscriberPull(n: Long) {
            TODO("handling the slow subscriber Pull (n elements)")
        }

        private fun publisherPush() {
            val a = sequence
            val s = actual
            // Starts a coroutine block for the SuspendingSequence
            launch(context) {
                try {
                    if (cancelled) {
                        return@launch
                    }
                    // This for loop is suspending and waits until the iterator hasNext() returns true
                    for (item in a) {
                        if (cancelled) {
                            return@launch
                        }
                        s.onNext(item)
                    }
                } catch (ex: Exception) {
                    s.onError(ex)
                }
                if (cancelled) {
                    return@launch
                }
                /** If all the items are produced, call [Subscriber.onComplete]*/
                s.onComplete()
            }
        }
    }
}