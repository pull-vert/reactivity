package io.khttp2.internal.common

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.createCoroutineUnchecked

@RestrictsSuspension
interface SuspendingSuplier<T> {
    suspend fun get() : T
}

@RestrictsSuspension
interface SupplierBuilder<T> : SuspendingSuplier<T> {
    suspend fun complete(value: T)

    suspend fun completeExceptionally(throwable: Throwable)
}

fun <T> buildSupplier(context: CoroutineContext = EmptyCoroutineContext, block: suspend SupplierBuilder<T>.() -> Unit): SupplierBuilder<T> =
        SupplierCoroutine<T>(context).apply {
            nextStep = block.createCoroutineUnchecked(receiver = this, completion = this)
        }

class SupplierCoroutine<T>(override val context: CoroutineContext) : SupplierBuilder<T>, Continuation<Unit> {
    enum class State { INITIAL, COMPUTING_GET, DONE }
    var state: State = State.INITIAL

    var value: T? = null
    var nextStep: Continuation<Unit>? = null // null when sequence complete

    // if (state == COMPUTING_GET) computeContinuation is Continuation<T>
    var computeContinuation: Continuation<T>? = null

    suspend fun computeGet(): T = suspendCoroutine { c ->
        state = State.COMPUTING_GET
        computeContinuation = c
        nextStep!!.resume(Unit)
    }

    // AbstractIterator implementation
    suspend override fun get(): T {
        when (state) {
            State.INITIAL -> return computeGet()
            State.DONE -> {
                state = State.INITIAL
                return value as T
            }
            else -> throw IllegalStateException("Recursive dependency detected -- already computing get")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun resumeSupplier() {
        when (state) {
            State.COMPUTING_GET -> {
                state = State.DONE
                computeContinuation!!.resume(value as T)
            }
            State.INITIAL -> {
                state = State.DONE
                nextStep!!.resume(Unit)
            }
            else -> throw IllegalStateException("Was not supposed to be computing next value. Spurious yield?")
        }
    }

    // Completion continuation implementation
    override fun resume(value: Unit) {
        nextStep = null
        resumeSupplier()
    }

    override fun resumeWithException(exception: Throwable) {
        nextStep = null
        state = State.DONE
        computeContinuation!!.resumeWithException(exception)
    }

    // Generator implementation
    suspend override fun complete(value: T): Unit = suspendCoroutine { c ->
        this.value = value
        nextStep = c
        resumeSupplier()
    }

    suspend override fun completeExceptionally(throwable: Throwable): Unit = suspendCoroutine { c ->
        nextStep = c
        computeContinuation!!.resumeWithException(throwable)
    }
}