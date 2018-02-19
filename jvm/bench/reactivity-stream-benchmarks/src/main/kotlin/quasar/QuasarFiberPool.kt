package quasar

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.strands.Strand
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor

// read http://docs.paralleluniverse.co/quasar/

object QuasarFiber : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
            PoolContinuation(continuation.context.fold(continuation, { cont, element ->
                if (element != this@QuasarFiber && element is ContinuationInterceptor)
                    element.interceptContinuation(cont) else cont
            }))
}

private class PoolContinuation<T>(
        val continuation: Continuation<T>
) : Continuation<T> by continuation {

    override fun resume(value: T) {
        if (Strand.isCurrentFiber()) continuation.resume(value)
        else Fiber<Unit>{ continuation.resume(value) }.start()
    }

    override fun resumeWithException(exception: Throwable) {
        if (Strand.isCurrentFiber()) continuation.resumeWithException(exception)
        else Fiber<Unit>{ continuation.resumeWithException(exception) }.start()
    }
}