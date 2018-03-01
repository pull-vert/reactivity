package reactivity.experimental

import kotlinx.coroutines.experimental.*
import kotlin.js.Promise

/**
 * Converts this solo value to the instance of [Promise].
 */
public fun <T> Solo<T>.toPromise(): Promise<T> = this@toPromise.toDeferred().asPromise()

/**
 * Converts this promise value to the instance of [Solo].
 */
public fun <T> Promise<T>.toSolo(): Solo<T> {
    val _solo = asDynamic().deferred
    @Suppress("UnsafeCastFromDynamic")
    return _solo ?: solo(start = CoroutineStart.UNDISPATCHED) { await() }
}