package reactivity.experimental

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
// TODO provide a custom ProducerScope impl that checks send is only called once, and throws exception otherwise !
fun <T> solo(
        scheduler: Scheduler,
        block: suspend ProducerScope<T>.() -> Unit
): Solo<T> = SoloImpl(publish(scheduler.context, block), scheduler)

/**
 * Subscribes to this [Solo] and performs the specified action for the unique received element.
 */
inline suspend fun <T> Solo<T>.consumeUnique(action: (T) -> Unit) {
    action.invoke(awaitSingle())
}

interface ISolo<T> : PublisherCommons<T> {
    // functions from WithCallbacks
    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T>
    override fun doOnNext(onNext: (T) -> Unit): Solo<T>
    override fun doOnError(onError: (Throwable) -> Unit): Solo<T>
    override fun doOnComplete(onComplete: () -> Unit): Solo<T>
    override fun doOnCancel(onCancel: () -> Unit): Solo<T>
    override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T>
    override fun doFinally(finally: () -> Unit): Solo<T>

    // function from WithPublishOn
    /**
     * Returns a [Solo] that is published with [initialScheduler] and the [delayError] option
     *
     * @param delayError if error should be delayed
     */
    override fun publishOn(delayError: Boolean): Solo<T>

    /**
     * Returns a [Solo] that is published with the provided [scheduler] and the [delayError] option
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     */
    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Solo<T>
}

internal class SoloImpl<T>(override val delegate: Publisher<T>,
                            override val initialScheduler: Scheduler)
    : ASolo<T>(), Publisher<T> by delegate

interface ISoloImpl<T> : ISolo<T> {

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Solo<*>)) {
            (delegate as PublisherWithCallbacks<T>).onSubscribeBlock = onSubscribe
            return this as Solo<T>
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onSubscribeBlock = onSubscribe
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnNext(onNext: (T) -> Unit): Solo<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Solo<*>)) {
            (delegate as PublisherWithCallbacks<T>).onNextBlock = onNext
            return this as Solo<T>
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onNextBlock = onNext
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnError(onError: (Throwable) -> Unit): Solo<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Solo<*>)) {
            (delegate as PublisherWithCallbacks<T>).onErrorBlock = onError
            return this as Solo<T>
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onErrorBlock = onError
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnComplete(onComplete: () -> Unit): Solo<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Solo<*>)) {
            (delegate as PublisherWithCallbacks<T>).onCompleteBlock = onComplete
            return this as Solo<T>
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCompleteBlock = onComplete
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnCancel(onCancel: () -> Unit): Solo<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Solo<*>)) {
            (delegate as PublisherWithCallbacks<T>).onCancelBlock = onCancel
            return this as Solo<T>
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCancelBlock = onCancel
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Solo<*>)) {
            (delegate as PublisherWithCallbacks<T>).onRequestBlock = onRequest
            return this as Solo<T>
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onRequestBlock = onRequest
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doFinally(finally: () -> Unit): Solo<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Solo<*>)) {
            (delegate as PublisherWithCallbacks<T>).finallyBlock = finally
            return this as Solo<T>
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.finallyBlock = finally
        return SoloImpl(publisherCallbacks, initialScheduler)
    }


    override fun publishOn(delayError: Boolean) = publishOn(initialScheduler, delayError)

    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Solo<T> = solo(scheduler) {
        val channel = PublisherPublishOn<T>(delayError, Int.MAX_VALUE)
        this@ISoloImpl.subscribe(channel)
        channel.consumeEach {
            send(it)
        }
    }
}