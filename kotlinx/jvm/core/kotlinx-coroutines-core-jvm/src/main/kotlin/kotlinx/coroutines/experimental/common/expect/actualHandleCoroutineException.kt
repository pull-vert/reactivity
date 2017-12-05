package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.common.Job
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext

actual fun handleCoroutineException(context: CoroutineContext, exception: Throwable) {
    context[CoroutineExceptionHandler]?.let {
        it.handleException(context, exception)
        return
    }
    // ignore CancellationException (they are normal means to terminate a coroutine)
    if (exception is CancellationException) return
    // try cancel job in the context
    context[Job]?.cancel(exception)
    // use additional extension handlers
    ServiceLoader.load(CoroutineExceptionHandler::class.java).forEach { handler ->
        handler.handleException(context, exception)
    }
    // use thread's handler
    val currentThread = Thread.currentThread()
    currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
}