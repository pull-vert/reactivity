package reactivity.experimental

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import kotlinx.coroutines.experimental.newCoroutineContext
import kotlinx.coroutines.experimental.reactive.awaitSingle
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import reactivity.experimental.core.*
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.experimental.startCoroutine

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
): Solo<T> = SoloImpl(SoloPublisherImpl(Publisher { subscriber ->
    val newContext = newCoroutineContext(scheduler.context)
    val coroutine = SoloCoroutine(newContext, subscriber)
    coroutine.initParentJob(scheduler.context[Job])
    subscriber.onSubscribe(coroutine) // do it first (before starting coroutine), to avoid unnecessary suspensions
    block.startCoroutine(coroutine, coroutine)
}, scheduler))

/**
 * Singleton builder for [Solo], a single (or empty) value Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
object SoloBuilder : SoloPublisherBuilder() {
    /**
     * Creates a [Solo] from a [value]
     *
     * @return the [Solo]<T> created
     *
     * @param T the type of the input [value]
     */
    @JvmStatic
    fun <T> fromValue(value: T): Solo<T> = SoloImpl(Companion.fromValue(value))

    /**
     * Creates a [Solo] from a [value]
     *
     * @return the [Solo]<T> created
     *
     * @param T the type of the input [value]
     */
    @JvmStatic
    fun <T> fromValue(scheduler: Scheduler, value: T): Solo<T> = SoloImpl(Companion.fromValue(scheduler, value))

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

/**
 * Single (or empty) value Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
interface Solo<T> : SoloPublisher<T> {
    // functions from WithCallbacks
    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T>

    override fun doOnNext(onNext: (T) -> Unit): Solo<T>
    override fun doOnError(onError: (Throwable) -> Unit): Solo<T>
    override fun doOnComplete(onComplete: () -> Unit): Solo<T>
    override fun doOnCancel(onCancel: () -> Unit): Solo<T>
    override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T>
    override fun doFinally(finally: () -> Unit): Solo<T>

    // function from WithPublishOn
    override fun publishOn(delayError: Boolean): Solo<T>
    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Solo<T>

    // Operators specific to Solo

    // Operators specific to Solo in JDK8

    fun toCompletableFuture(): CompletableFuture<T>
    fun toCompletableFuture(scheduler: Scheduler): CompletableFuture<T>
}

internal class SoloImpl<T> internal constructor(del: SoloPublisherImpl<T>)
    : SoloPublisherImpl<T>(del.delegate, del.initialScheduler), Solo<T> {

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