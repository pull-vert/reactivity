package kotlinx.coroutines.experimental.common.expect

actual typealias CancellationException = java.util.concurrent.CancellationException

actual class CompletionHandlerException actual constructor(message: String, cause: Throwable) : kotlin.RuntimeException(message, cause)

actual fun Throwable.initCauseThrowable(cause: Throwable): Throwable? = this@initCauseThrowable.initCause(cause)

actual fun Throwable.addSuppressedThrowable(cause: Throwable) = this@addSuppressedThrowable.addSuppressed(cause)

actual class IllegalStateException actual constructor(message: String?, cause: Throwable?) : kotlin.IllegalStateException(message, cause)