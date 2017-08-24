package reactivity.experimental

interface Exceptions {
    companion object {
        /**
         * An exception that is propagated upward and considered as "fatal" as per Reactive
         * Stream limited list of exceptions allowed to bubble. It is not meant to be common
         * error resolution but might assist implementors in dealing with boundaries (queues,
         * combinations and async).
         *
         * @return a [RuntimeException] that can be identified via [.isCancel]
         * @see .isCancel
         */
        fun failWithCancel(): RuntimeException {
            return CancelException()
        }

        /**
         * Prepare an unchecked [RuntimeException] that will bubble upstream if thrown
         * by an operator.
         *
         *This method invokes [.throwIfFatal].
         *
         * @param t the root cause
         *
         * @return an unchecked exception that should choose bubbling up over error callback
         * path
         */
        fun bubble(t: Throwable): RuntimeException {
            throwIfFatal(t)
            return BubblingException(t)
        }

        /**
         * Throws a particular `Throwable` only if it belongs to a push of "fatal" error
         * varieties. These varieties are as follows:
         *  * `BubblingException` (as detectable by [.isBubbling])
         *  * `ErrorCallbackNotImplemented` (as detectable by [.isErrorCallbackNotImplemented])
         *  * [VirtualMachineError]  * [ThreadDeath]  * [LinkageError]
         *
         * @param t the exception to evaluate
         */
        fun throwIfFatal(t: Throwable) {
            if (t is BubblingException) {
                throw t
            }
            throwIfJvmFatal(t)
        }

        /**
         * Throws a particular `Throwable` only if it belongs to a push of "fatal" error
         * varieties native to the JVM. These varieties are as follows:
         *   * [VirtualMachineError]  * [ThreadDeath]
         *  * [LinkageError]
         *
         * @param t the exception to evaluate
         */
        fun throwIfJvmFatal(t: Throwable) {
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
         * Unwrap a particular `Throwable` only if it is was wrapped via
         * [bubble][.bubble] or [propagate][.propagate].
         *
         * @param t the exception to unwrap
         *
         * @return the unwrapped exception
         */
        fun unwrap(t: Throwable): Throwable {
            var _t = t
            while (_t is ReactiveException) {
                _t = _t.cause!!
            }
            return _t
        }

        /**
         * Check if the given error is a [callback not implemented][.errorCallbackNotImplemented]
         * exception, in which case its [cause][Throwable.getCause] will be the propagated
         * error that couldn't be processed.
         *
         * @param t the [Throwable] error to check
         * @return true if given [Throwable] is a callback not implemented exception.
         */
        fun isErrorCallbackNotImplemented(t: Throwable): Boolean {
            return t is ErrorCallbackNotImplemented
        }
    }
}

/**
 * An exception that is propagated downward through [org.reactivestreams.Subscriber.onError]
 */
internal open class ReactiveException : RuntimeException {

    constructor(cause: Throwable) : super(cause) {}

    constructor(message: String) : super(message) {}

    @Synchronized override fun fillInStackTrace(): Throwable {
        return if (cause != null)
            (cause as java.lang.Throwable).fillInStackTrace()
        else
            super.fillInStackTrace()
    }

    companion object {

        private const val serialVersionUID = 2491425227432776143L
    }
}

internal open class BubblingException : ReactiveException {

    constructor(message: String) : super(message) {}

    constructor(cause: Throwable) : super(cause) {}

    companion object {

        private const val serialVersionUID = 2491425277432776142L
    }
}

internal class ErrorCallbackNotImplemented(cause: Throwable) : UnsupportedOperationException(cause) {

    @Synchronized override fun fillInStackTrace(): Throwable {
        return this
    }

    companion object {

        private const val serialVersionUID = 2491425227432776143L
    }
}

/**
 * An error signal from downstream subscribers consuming data when their state is
 * denying any additional event.
 *
 * @author Stephane Maldini
 */
internal class CancelException : BubblingException("The subscriber has denied dispatching") {
    companion object {

        private const val serialVersionUID = 2491425227432776144L
    }

}