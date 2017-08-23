package reactivity.internal.util

import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicLongFieldUpdater

// Extensions for Subscription
fun Subscription.validate(n: Long): Boolean {
    if (n == 0L) {
        return false
    }
    if (n < 0) {
        throw IllegalArgumentException(
                "Spec. Rule 3.9 - Cannot request a non strictly positive number: " + n);
    }
    return true
}

/**
 * Concurrent addition bound to Long.MAX_VALUE.
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