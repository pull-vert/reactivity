package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.AbstractCancellableContinuation
import kotlinx.coroutines.experimental.common.CancellableContinuationCommon
import kotlin.coroutines.experimental.Continuation

expect fun <T> Continuation<T>.resumeCancellable(value: T)

expect fun <T> Continuation<T>.resumeCancellableWithException(exception: Throwable)

expect fun <T> Continuation<T>.resumeDirect(value: T)

expect fun <T> Continuation<T>.resumeDirectWithException(exception: Throwable)

expect fun <T> Continuation<T>.resumeMode(value: T, mode: Int)

expect fun <T> Continuation<T>.resumeWithExceptionMode(exception: Throwable, mode: Int)

expect interface CancellableContinuation<in T> : CancellableContinuationCommon<T>

expect class CancellableContinuationImpl<in T>(delegate: Continuation<T>,
                                               resumeMode: Int) : AbstractCancellableContinuation<T>