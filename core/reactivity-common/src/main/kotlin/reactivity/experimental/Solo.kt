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

internal open class SoloImpl<T>(val delegate: Publisher<T>,
                           override val initialScheduler: Scheduler)
    : Solo<T>, Publisher<T> by delegate {

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onSubscribeBlock = onSubscribe
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onSubscribeBlock = onSubscribe
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnNext(onNext: (T) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onNextBlock = onNext
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onNextBlock = onNext
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnError(onError: (Throwable) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onErrorBlock = onError
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onErrorBlock = onError
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnComplete(onComplete: () -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onCompleteBlock = onComplete
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCompleteBlock = onComplete
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnCancel(onCancel: () -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onCancelBlock = onCancel
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCancelBlock = onCancel
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onRequestBlock = onRequest
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onRequestBlock = onRequest
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doFinally(finally: () -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).finallyBlock = finally
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.finallyBlock = finally
        return SoloImpl(publisherCallbacks, initialScheduler)
    }


    override fun publishOn(delayError: Boolean) = publishOn(initialScheduler, delayError)

    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Solo<T> = solo(scheduler) {
        val channel = PublisherPublishOn<T>(delayError, Int.MAX_VALUE)
        this@SoloImpl.subscribe(channel)
        channel.consumeEach {
            send(it)
        }
    }
}