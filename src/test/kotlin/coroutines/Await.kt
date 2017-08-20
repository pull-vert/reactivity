package coroutines

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.*

/**
* Coroutine builder. Here is a simplified version of launch to "fire and forget" a coroutine
*/
fun launch(contextParam: CoroutineContext = newSingleThreadContext("singleThread"), block: suspend () -> Unit) {
    block.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext get() = contextParam
        override fun resume(value: Unit) {}
        override fun resumeWithException(e: Throwable) { println("Coroutine failed: $e") }
    })
}

fun newSingleThreadContext(name: String) = ThreadContext(name)

private val thisThreadContext = ThreadLocal<ThreadContext>()

private val executor = Executors.newSingleThreadScheduledExecutor {
    Thread(it, "scheduler").apply { isDaemon = true }
}

class ThreadContext(
        name: String
) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    val threadNo = AtomicInteger()
    val exec: ScheduledExecutorService = executor/*Executors.newSingleThreadScheduledExecutor { target ->
        thread(start = false, isDaemon = true, name = name + "-" + threadNo.incrementAndGet()) {
            thisThreadContext.set(this@ThreadContext)
            target.run()
        }
    }*/

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
            ThreadContinuation(continuation.context.fold(continuation, { cont, element ->
                if (element != this@ThreadContext && element is ContinuationInterceptor)
                    element.interceptContinuation(cont) else cont
            }))

    private inner class ThreadContinuation<T>(val continuation: Continuation<T>) : Continuation<T> by continuation {
        override fun resume(value: T) {
            if (isContextThread()) continuation.resume(value)
            else exec.execute { continuation.resume(value) }
        }

        override fun resumeWithException(exception: Throwable) {
            if (isContextThread()) continuation.resumeWithException(exception)
            else exec.execute { continuation.resumeWithException(exception) }
        }
    }

    private fun isContextThread() = thisThreadContext.get() == this@ThreadContext
}

suspend fun delay(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Unit = suspendCoroutine { cont ->
    executor.schedule({ cont.resume(Unit) }, time, unit)
}