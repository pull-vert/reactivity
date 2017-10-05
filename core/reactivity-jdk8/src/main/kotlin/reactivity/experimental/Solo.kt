package reactivity.experimental

import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import kotlinx.coroutines.experimental.reactive.awaitSingle
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import reactivity.experimental.core.*
import java.util.concurrent.CompletableFuture

/**
 * Creates cold reactive [Solo] that runs a given [block] in a coroutine.
 * Every time the returned publisher is subscribed, it starts a new coroutine in the specified [scheduler].
 * Coroutine emits items with `produce`. Unsubscribing cancels running coroutine.
 *
 * Invocation of `produce` is suspended appropriately when subscribers apply back-pressure and to ensure that
 * `onNext` is not invoked concurrently.
 *
 * | **Coroutine action**                         | **Signal to subscriber**
 * | -------------------------------------------- | ------------------------
 * | `value in end of coroutine is not null`      | `onNext`
 * | Normal completion or `close` without cause   | `onComplete`
 * | Failure with exception or `close` with cause | `onError`
 */
fun <T> solo(
        scheduler: Scheduler,
        block: suspend ProducerScope<T>.() -> Unit
): Solo<T> = SoloImpl(defaultSoloPublisher(scheduler, block))

/**
 * Single (or empty) value Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
abstract class Solo<T> protected constructor() : SoloPublisher<T> {

    /**
     * Builder for [Solo], a single (or empty) value Reactive Stream [Publisher]
     *
     * @author Frédéric Montariol
     */
    companion object : SoloPublisherBuilder() {
        /**
         * Creates a [Solo] from a [value]
         *
         * @return the [Solo]<T> created
         *
         * @param T the type of the input [value]
         */
        @JvmStatic
        fun <T> fromValue(value: T): Solo<T> = SoloImpl(SoloPublisherBuilder.fromValue(value))

        /**
         * Creates a [Solo] from a [value]
         *
         * @return the [Solo]<T> created
         *
         * @param T the type of the input [value]
         */
        @JvmStatic
        fun <T> fromValue(scheduler: Scheduler, value: T): Solo<T> = SoloImpl(SoloPublisherBuilder.fromValue(scheduler, value))

        /**
         * Creates a [Solo] from a [completableFuture]
         *
         * @return the [Solo]<T> created
         *
         * @param T the type of the input [completableFuture]
         */
        @JvmStatic
        fun <T> fromCompletableFuture(completableFuture: CompletableFuture<T>)
                = fromCompletableFuture(DEFAULT_SCHEDULER, completableFuture)

        /**
         * Creates a [Solo] from a [completableFuture]
         *
         * @return the [Solo]<T> created
         *
         * @param T the type of the input [completableFuture]
         */
        @JvmStatic
        fun <T> fromCompletableFuture(scheduler: Scheduler, completableFuture: CompletableFuture<T>) = solo(scheduler) {
            send(completableFuture.await())
        }
    }

    // functions from WithCallbacks
    abstract override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T>

    abstract override fun doOnNext(onNext: (T) -> Unit): Solo<T>
    abstract override fun doOnError(onError: (Throwable) -> Unit): Solo<T>
    abstract override fun doOnComplete(onComplete: () -> Unit): Solo<T>
    abstract override fun doOnCancel(onCancel: () -> Unit): Solo<T>
    abstract override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T>
    abstract override fun doFinally(finally: () -> Unit): Solo<T>

    // function from WithPublishOn
    abstract override fun publishOn(delayError: Boolean): Solo<T>
    abstract override fun publishOn(scheduler: Scheduler, delayError: Boolean): Solo<T>

    // Operators specific to Solo

    // Operators specific to Solo in JDK8

    abstract fun toCompletableFuture(): CompletableFuture<T>
    abstract fun toCompletableFuture(scheduler: Scheduler): CompletableFuture<T>
}

internal class SoloImpl<T> internal constructor(private val del: DefaultSoloPublisher<T>)
    : Solo<T>(), DefaultSoloPublisher<T>, Publisher<T> by del.delegate {

    override val delegate: Publisher<T>
        get() = del.delegate
    override val initialScheduler: Scheduler
        get() = del.initialScheduler

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit) = SoloImpl(super.doOnSubscribe(onSubscribe))
    override fun doOnNext(onNext: (T) -> Unit) = SoloImpl(super.doOnNext(onNext))
    override fun doOnError(onError: (Throwable) -> Unit) = SoloImpl(super.doOnError(onError))
    override fun doOnComplete(onComplete: () -> Unit) = SoloImpl(super.doOnComplete(onComplete))
    override fun doOnCancel(onCancel: () -> Unit) = SoloImpl(super.doOnCancel(onCancel))
    override fun doOnRequest(onRequest: (Long) -> Unit) = SoloImpl(super.doOnRequest(onRequest))
    override fun doFinally(finally: () -> Unit) = SoloImpl(super.doFinally(finally))
    override fun publishOn(delayError: Boolean) = SoloImpl(super.publishOn(delayError))
    override fun publishOn(scheduler: Scheduler, delayError: Boolean) = SoloImpl(super.publishOn(scheduler, delayError))

    override fun toCompletableFuture() = toCompletableFuture(initialScheduler)

    override fun toCompletableFuture(scheduler: Scheduler) = future(scheduler.context) {
        awaitSingle()
    }
}