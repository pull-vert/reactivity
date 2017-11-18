package reactivity.experimental

expect internal fun throwIfJvmFatal(t: Throwable)

expect internal open class ReactiveException : RuntimeException {
    constructor()
    constructor(message: String)
    constructor(cause: Throwable)
}

expect internal class ErrorCallbackNotImplemented : UnsupportedOperationException {
    constructor(cause: Throwable)
}

expect internal fun addSuppressed(t: Throwable, exception: Throwable)
