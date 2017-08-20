package io.khttp2.internal.common

import java.util.function.Supplier

private enum class State {
    Ready,
    NotReady,
    Done,
    Failed
}

/**
 * A base class to simplify implementing Suppliers so that implementations only have to implement [computeValue]
 * to implement the supplier, calling [done] when the supply is complete.
 */
abstract class AbstractSupplier<T>: Supplier<T> {
    private var state = State.NotReady
    private var value: T? = null

    fun hasValue(): Boolean {
        require(state != State.Failed)
        return when (state) {
            State.Done -> false
            State.Ready -> true
            else -> tryToComputeValue()
        }
    }

    override fun get(): T {
        if (!hasValue()) throw NoSuchElementException()
        state = State.NotReady
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private fun tryToComputeValue(): Boolean {
        state = State.Failed
        computeValue()
        return state == State.Ready
    }

    /**
     * Computes the next item in the iterator.
     *
     * This callback method should call :
     *
     * * [done] to indicate there are no more elements
     *
     * Failure to call either method will result in the iteration terminating with a failed state
     */
    abstract protected fun computeValue()

    /**
     * Sets the next value in the iteration, called from the [computeValue] function
     */
    protected fun setValue(value: T) {
        this.value = value
        state = State.Ready
    }

    /**
     * Sets the state to done so that the iteration terminates.
     */
    protected fun done() {
        state = State.Done
    }
}