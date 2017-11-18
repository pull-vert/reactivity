package reactivity.experimental

expect internal fun throwIfJvmFatal(t: Throwable)

expect internal open class ReactiveException : RuntimeException {
    constructor(message: String)
    constructor(cause: Throwable)
}

expect internal class ErrorCallbackNotImplemented(cause: Throwable) : UnsupportedOperationException

expect internal fun Throwable.addSuppress(ex: Throwable)
