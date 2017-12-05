package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.common.*
import kotlin.coroutines.experimental.Continuation

actual fun <T> Continuation<T>.resumeCancellable(value: T) = when (this) {
    is DispatchedContinuation -> resumeCancellable(value)
    else -> resume(value)
}

actual fun <T> Continuation<T>.resumeCancellableWithException(exception: Throwable) = when (this) {
    is DispatchedContinuation -> resumeCancellableWithException(exception)
    else -> resumeWithException(exception)
}

actual fun <T> Continuation<T>.resumeDirect(value: T) = when (this) {
    is DispatchedContinuation -> continuation.resume(value)
    else -> resume(value)
}

actual fun <T> Continuation<T>.resumeDirectWithException(exception: Throwable) = when (this) {
    is DispatchedContinuation -> continuation.resumeWithException(exception)
    else -> resumeWithException(exception)
}

actual fun <T> Continuation<T>.resumeMode(value: T, mode: Int) {
    when (mode) {
        MODE_ATOMIC_DEFAULT -> resume(value)
        MODE_CANCELLABLE -> resumeCancellable(value)
        MODE_DIRECT -> resumeDirect(value)
        MODE_UNDISPATCHED -> (this as DispatchedContinuation).resumeUndispatched(value)
        MODE_IGNORE -> {}
        else -> error("Invalid mode $mode")
    }
}

actual fun <T> Continuation<T>.resumeWithExceptionMode(exception: Throwable, mode: Int) {
    when (mode) {
        MODE_ATOMIC_DEFAULT -> resumeWithException(exception)
        MODE_CANCELLABLE -> resumeCancellableWithException(exception)
        MODE_DIRECT -> resumeDirectWithException(exception)
        MODE_UNDISPATCHED -> (this as DispatchedContinuation).resumeUndispatchedWithException(exception)
        MODE_IGNORE -> {}
        else -> error("Invalid mode $mode")
    }
}

actual interface CancellableContinuation<in T> : CancellableContinuationCommon<T> {
    /**
     * Resumes this continuation with a given [value] in the invoker thread without going though
     * [dispatch][CoroutineDispatcher.dispatch] function of the [CoroutineDispatcher] in the [context].
     * This function is designed to be used only by the [CoroutineDispatcher] implementations themselves.
     * **It should not be used in general code**.
     */
    public fun CoroutineDispatcher.resumeUndispatched(value: T)

    /**
     * Resumes this continuation with a given [exception] in the invoker thread without going though
     * [dispatch][CoroutineDispatcher.dispatch] function of the [CoroutineDispatcher] in the [context].
     * This function is designed to be used only by the [CoroutineDispatcher] implementations themselves.
     * **It should not be used in general code**.
     */
    public fun CoroutineDispatcher.resumeUndispatchedWithException(exception: Throwable)
}

actual class CancellableContinuationImpl<in T> actual constructor(delegate: Continuation<T>,
                                               resumeMode: Int) : AbstractCancellableContinuation<T>(delegate, resumeMode), CancellableContinuation<T> {
    override fun CoroutineDispatcher.resumeUndispatched(value: T) {
        val dc = delegate as? DispatchedContinuation
        resumeImpl(value, if (dc?.dispatcher === this) MODE_UNDISPATCHED else resumeMode)
    }

    override fun CoroutineDispatcher.resumeUndispatchedWithException(exception: Throwable) {
        val dc = delegate as? DispatchedContinuation
        resumeImpl(CompletedExceptionally(exception), if (dc?.dispatcher === this) MODE_UNDISPATCHED else resumeMode)
    }

    override fun nameString(): String =
            "CancellableContinuation(${delegate.toDebugString()})"
}