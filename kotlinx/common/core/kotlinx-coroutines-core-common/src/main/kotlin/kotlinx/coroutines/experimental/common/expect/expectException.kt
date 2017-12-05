package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.Job

expect open class CancellationException(message: String?) : kotlin.IllegalStateException

/**
 * This exception gets thrown if an exception is caught while processing [CompletionHandler] invocation for [Job].
 */
expect class CompletionHandlerException(message: String, cause: Throwable) : RuntimeException

expect fun Throwable.initCauseThrowable(cause: Throwable): Throwable?

expect fun Throwable.addSuppressedThrowable(cause: Throwable)

expect class IllegalStateException(message: String?, cause: Throwable?) : kotlin.IllegalStateException