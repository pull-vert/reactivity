package io.khttp2.internal.common

import java.util.function.Supplier
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.RestrictsSuspension
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.createCoroutineUnchecked
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn

@RestrictsSuspension
interface SupplierBuilder<T> : Supplier<T> {
    suspend fun complete(value: T)

    suspend fun completeExceptionally(throwable: Throwable)
}

fun <T> buildSupplier(block: suspend SupplierBuilder<T>.() -> Unit): SupplierBuilder<T> {
    return SupplierCoroutine<T>().apply {
        nextStep = block.createCoroutineUnchecked(receiver = this, completion = this)
    }
}

class SupplierCoroutine<T> : AbstractSupplier<T>(), SupplierBuilder<T>, Continuation<Unit> {

    lateinit var nextStep: Continuation<Unit>

    // AbstractIterator implementation
    override fun computeValue() { nextStep.resume(Unit) }

    // Completion continuation implementation
    override val context: CoroutineContext get() = EmptyCoroutineContext
    override fun resume(value: Unit) { done() }
    override fun resumeWithException(exception: Throwable) { throw exception }

    // Generator implementation
    suspend override fun complete(value: T) {
        setValue(value)
        return suspendCoroutineOrReturn { cont ->
            nextStep = cont
            COROUTINE_SUSPENDED
        }
    }

    suspend override fun completeExceptionally(throwable: Throwable) {
        nextStep.resumeWithException(throwable)
        return suspendCoroutineOrReturn { cont ->
            nextStep = cont
            COROUTINE_SUSPENDED
        }
    }
}