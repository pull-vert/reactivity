package coroutines

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

fun launchSimple(context: CoroutineContext, block: suspend () -> Unit) =
        block.startCoroutine(StandaloneCoroutine(context))

private class StandaloneCoroutine(override val context: CoroutineContext): Continuation<Unit> {
    override fun resume(value: Unit) {}

    override fun resumeWithException(exception: Throwable) {
        val currentThread = Thread.currentThread()
        currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
    }
}