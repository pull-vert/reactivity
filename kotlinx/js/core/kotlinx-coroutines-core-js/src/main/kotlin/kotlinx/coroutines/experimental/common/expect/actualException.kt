package kotlinx.coroutines.experimental.common.expect


actual open class CancellationException actual constructor(message: String?)
    : kotlin.IllegalStateException(message)

actual class CompletionHandlerException actual constructor(message: String, cause: Throwable) : kotlin.RuntimeException(message)

actual fun Throwable.initCauseThrowable(cause: Throwable): Throwable? = null

actual fun Throwable.addSuppressedThrowable(cause: Throwable) {}

actual class IllegalStateException actual constructor(message: String?, cause: Throwable?) : kotlin.IllegalStateException(message)