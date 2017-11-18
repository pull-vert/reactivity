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
         */
        fun failWithCancel(): RuntimeException {
            return reactivity.experimental.CancelException()
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
            reactivity.experimental.Exceptions.Companion.throwIfFatal(t)
            return reactivity.experimental.BubblingException(t)
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
            if (t is reactivity.experimental.BubblingException) {
                throw t
            }
            reactivity.experimental.throwIfJvmFatal(t)
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
            while (_t is reactivity.experimental.ReactiveException) {
                _t = _t.cause!!
            }
            return _t
        }

        /**
         * Check if the given error is a [callback not implemented][.errorCallbackNotImplemented]
         * exception, in which case its [cause][Throwable.cause] will be the propagated
         * error that couldn't be processed.
         *
         * @param t the [Throwable] error to check
         * @return true if given [Throwable] is a callback not implemented exception.
         */
        fun isErrorCallbackNotImplemented(t: Throwable): Boolean {
            return t is reactivity.experimental.ErrorCallbackNotImplemented
        }

        /**
         * Return an [UnsupportedOperationException] indicating that the error callback
         * on a subscriber was not implemented, yet an error was propagated.
         *
         * @param cause original error not processed by a receiver.
         * @return an [UnsupportedOperationException] indicating the error callback was
         * not implemented and holding the original propagated error.
         * @see isErrorCallbackNotImplemented
         */
        fun errorCallbackNotImplemented(cause: Throwable): UnsupportedOperationException {
            return reactivity.experimental.ErrorCallbackNotImplemented(cause)
        }
    }
}

internal open class BubblingException : reactivity.experimental.ReactiveException {

    constructor(message: String) : super(message)

    constructor(cause: Throwable) : super(cause)

    companion object {

        private const val serialVersionUID = 2491425277432776142L
    }
}

/**
 * An error signal from downstream subscribers consuming data when their state is
 * denying any additional event.
 *
 */
internal class CancelException : reactivity.experimental.BubblingException("The subscriber has denied dispatching") {
    companion object {

        private const val serialVersionUID = 2491425227432776144L
    }
}