package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.*
import kotlin.coroutines.experimental.Continuation

actual fun <T> Continuation<T>.resumeCancellable(value: T) = this.resume(value)

actual fun <T> Continuation<T>.resumeCancellableWithException(exception: Throwable) = this.resumeWithException(exception)

actual fun <T> Continuation<T>.resumeDirect(value: T) = this.resume(value)

actual fun <T> Continuation<T>.resumeDirectWithException(exception: Throwable) = this.resumeWithException(exception)

actual fun <T> Continuation<T>.resumeMode(value: T, mode: Int) {
    when (mode) {
        MODE_ATOMIC_DEFAULT -> resume(value)
        MODE_CANCELLABLE -> resumeCancellable(value)
        MODE_DIRECT -> resumeDirect(value)
        MODE_IGNORE -> {}
        else -> error("Invalid mode $mode")
    }
}

actual fun <T> Continuation<T>.resumeWithExceptionMode(exception: Throwable, mode: Int) {
    when (mode) {
        MODE_ATOMIC_DEFAULT -> resumeWithException(exception)
        MODE_CANCELLABLE -> resumeCancellableWithException(exception)
        MODE_DIRECT -> resumeDirectWithException(exception)
        MODE_IGNORE -> {}
        else -> error("Invalid mode $mode")
    }
}

actual interface CancellableContinuation<in T> : CancellableContinuationCommon<T>

actual class CancellableContinuationImpl<in T> actual constructor(delegate: Continuation<T>,
                                                                  resumeMode: Int) : AbstractCancellableContinuation<T>(delegate, resumeMode), CancellableContinuation<T> {
    override fun nameString(): String =
            "CancellableContinuation($delegate)"
}