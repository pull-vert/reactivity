package reactivity.experimental.expect

/**
 * Throws a particular `Throwable` only if it belongs to a push of "fatal" error
 * varieties native to the JVM. These varieties are as follows:
 *   * [VirtualMachineError]  * [ThreadDeath]
 *  * [LinkageError]
 *
 * @param t the exception to evaluate
 */
actual internal fun throwIfJvmFatal(t: Throwable) {
    if (t is VirtualMachineError) {
        throw t
    }
    if (t is ThreadDeath) {
        throw t
    }
    if (t is LinkageError) {
        throw t
    }
}

/**
 * An exception that is propagated downward through [org.reactivestreams.Subscriber.onError]
 */
actual internal open class ReactiveException : RuntimeException {
    actual constructor(cause: Throwable) : super(cause)

    actual constructor(message: String) : super(message)

    @Synchronized override fun fillInStackTrace(): Throwable {
        return if (cause != null)
            cause!!.fillInStackTrace()
        else
            super.fillInStackTrace()
    }

    companion object {
        private const val serialVersionUID = 2491425227432776143L
    }
}

actual internal class ErrorCallbackNotImplemented actual constructor(cause: Throwable) : UnsupportedOperationException(cause) {

    @Synchronized override fun fillInStackTrace(): Throwable {
        return this
    }

    companion object {
        private const val serialVersionUID = 2491425227432776143L
    }
}

actual internal fun Throwable.addSuppress(ex: Throwable) = this@addSuppress.addSuppressed(ex)