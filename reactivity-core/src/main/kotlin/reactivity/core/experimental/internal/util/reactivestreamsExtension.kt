package reactivity.core.experimental.internal.util

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactivity.core.experimental.Exceptions
import java.util.concurrent.atomic.AtomicLongFieldUpdater

// Extensions for Subscription
fun Subscription.validateRequested(n: Long): Boolean {
    if (n == 0L) {
        return false
    }
    if (n < 0) {
        throw IllegalArgumentException(
                "Spec. Rule 3.9 - Cannot request a non strictly positive number: " + n)
    }
    return true
}

/**
 * Concurrent addition bound to [Long.MAX_VALUE].
 * Any concurrent write will "happen before" this operation.
 *
 * @param <T> the parent instance type
 * @param updater  current field updater
 * @param instance current instance to update
 * @param toAdd    delta to add
 * @return value before addition or Long.MAX_VALUE
</T> */
fun <T> Subscription.getAndAddCap(updater: AtomicLongFieldUpdater<T>, instance: T, toAdd: Long): Long {
    var r: Long
    var u: Long
    do {
        r = updater.get(instance)
        if (r == java.lang.Long.MAX_VALUE) {
            return java.lang.Long.MAX_VALUE
        }
        u = addCap(r, toAdd)
    } while (!updater.compareAndSet(instance, r, u))

    return r
}

/**
 * Cap an addition to Long.MAX_VALUE
 *
 * @param a left operand
 * @param b right operand
 *
 * @return Addition result or Long.MAX_VALUE if overflow
 */
fun Subscription.addCap(a: Long, b: Long): Long {
    val res = a + b
    return if (res < 0L) {
        java.lang.Long.MAX_VALUE
    } else res
}

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

    override fun cancel() {
        // deliberately no op
    }

    override fun request(n: Long) {
        // deliberately no op
    }
}

private enum class CancelledSubscription : Subscription {
    INSTANCE;

    override fun cancel() {
        // deliberately no op
    }

    override fun request(n: Long) {
        // deliberately no op
    }
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
fun Subscriber<*>.onOperatorError(error: Throwable): Throwable {
    return onOperatorError(null, error, null)
}

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
fun Subscriber<*>.onOperatorError(subscription: Subscription?, error: Throwable): Throwable {
    return onOperatorError(subscription, error, null)
}

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
            (t as java.lang.Throwable).addSuppressed(dataSignal)
        }
        //do not wrap original value to avoid strong references
        /*else {
            }*/
    }
    return t
}

/**
 * An unexpected event is about to be dropped.
 *
 * @param <T> the dropped value type
 * @param t the dropped data
</T> */
fun <T> Subscriber<*>.onNextDropped(t: T) {
    throw Exceptions.failWithCancel()
}

/**
 * An unexpected exception is about to be dropped.
 *
 * @param e the dropped exception
 */
fun Subscriber<*>.onErrorDropped(e: Throwable) {
   throw Exceptions.bubble(e)
}

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

///**
// * Method for [SubscriberCallbacks] to deal with a doFinally
// * callback that fails during onError. It drops the error to the global hook.
// *
// *  * The callback failure is thrown immediately if fatal.
// *  * [onOperatorError] is
// * called, adding the original error as suppressed
// *  * [onErrorDropped] is called
// *
// * @param callbackFailure the afterTerminate callback failure
// * @param originalError the onError throwable
// */
//fun Subscriber<*>.afterErrorWithFailure(callbackFailure: Throwable, originalError: Throwable) {
//    Exceptions.throwIfFatal(callbackFailure)
//    val _e = onOperatorError(null, callbackFailure, originalError)
//    onErrorDropped(_e)
//}
//
///**
// * Method for [SubscriberCallbacks] to deal with a doFinally
// * callback that fails during onComplete. It drops the error to the global hook.
// *
// *  * The callback failure is thrown immediately if fatal.
// *  * [onOperatorError] is called
// *  * [onErrorDropped] is called
// *
// * @param callbackFailure the afterTerminate callback failure
// */
//fun Subscriber<*>.afterCompleteOrCancelWithFailure(callbackFailure: Throwable) {
//    Exceptions.throwIfFatal(callbackFailure)
//    val _e = onOperatorError(callbackFailure)
//    onErrorDropped(_e)
//}