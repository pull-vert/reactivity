package reactivity.experimental

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

// Extensions for Subscriber

/**
 * Calls onSubscribe on the target Subscriber with the empty instance followed by a call to onError with the
 * supplied error.
 *
 * @param s target Subscriber to error
 * @param e the actual error
 */
fun Subscriber<*>.errorInOnSubscribe(s: Subscriber<*>, e: Throwable) {
    s.onSubscribe(EmptySubscription.INSTANCE)
    s.onError(e)
}

private enum class EmptySubscription : Subscription {
    INSTANCE;
    override fun cancel() { } // deliberately no op
    override fun request(n: Long) { } // deliberately no op
}

private enum class CancelledSubscription : Subscription {
    INSTANCE;
    override fun cancel() { } // deliberately no op
    override fun request(n: Long) { } // deliberately no op
}

/**
 * A singleton Subscription that represents a cancelled subscription instance and
 * should not be leaked to clients as it represents a terminal state. <br></br> If
 * algorithms need to hand out a subscription, replace this with a singleton
 * subscription because there is no standard way to tell if a Subscription is cancelled
 * or not otherwise.
 *
 * @return a singleton noop [Subscription] to be used as an inner representation
 * of the cancelled state
 */
fun Subscriber<*>.cancelledSubscription(): Subscription {
    return CancelledSubscription.INSTANCE
}

/**
 * Map an "operator" error. The
 * result error will be passed via onError to the operator downstream after
 * checking for fatal error via
 * [Exceptions.throwIfFatal].
 *
 * @param error the callback or operator error
 * @return mapped [Throwable]
 */
fun Subscriber<*>.onOperatorError(error: Throwable) = onOperatorError(null, error, null)

/**
 * Map an "operator" error given an operator parent [Subscription]. The
 * result error will be passed via onError to the operator downstream.
 * [Subscription] will be cancelled after checking for fatal error via
 * [throwIfFatal].
 *
 * @param subscription the linked operator parent [Subscription]
 * @param error the callback or operator error
 * @return mapped [Throwable]
 */
fun Subscriber<*>.onOperatorError(subscription: Subscription?, error: Throwable) = onOperatorError(subscription, error, null)

/**
 * Map an "operator" error given an operator parent [Subscription]. The
 * result error will be passed via onError to the operator downstream.
 * [Subscription] will be cancelled after checking for fatal error via
 * [throwIfFatal]. Takes an additional signal, which
 * can be added as a suppressed exception if it is a [Throwable]
 *
 * @param subscription the linked operator parent [Subscription]
 * @param error the callback or operator error
 * @param dataSignal the value (onNext or onError) signal processed during failure
 * @return mapped [Throwable]
 */
fun Subscriber<*>.onOperatorError(subscription: Subscription?,
                                  error: Throwable,
                                  dataSignal: Any?): Throwable {

    Exceptions.throwIfFatal(error)
    subscription?.cancel()

    val t = Exceptions.unwrap(error)
    if (dataSignal != null) {
        if (dataSignal !== t && dataSignal is Throwable) {
            t.addSuppressed(dataSignal)
        }
        //do not wrap original value to avoid strong references
    }
    return t
}

/**
 * An unexpected event is about to be dropped.
 *
 */
fun Subscriber<*>.onNextDropped() { throw Exceptions.failWithCancel() }

/**
 * An unexpected exception is about to be dropped.
 *
 * @param e the dropped exception
 */
fun Subscriber<*>.onErrorDropped(e: Throwable) { throw Exceptions.bubble(e) }

/**
 * Check Subscription current state and cancel new Subscription if current is push,
 * or return true if ready to subscribe.
 *
 * @param current current Subscription, expected to be null
 * @param next new Subscription
 * @return true if Subscription can be used
 */
fun Subscriber<*>.validateSubscription(current: Subscription?, next: Subscription): Boolean {
    if (current != null) {
        next.cancel()
        //reportSubscriptionSet()
        return false
    }
    return true
}