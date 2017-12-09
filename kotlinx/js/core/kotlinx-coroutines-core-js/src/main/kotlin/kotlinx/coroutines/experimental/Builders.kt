package kotlinx.coroutines.experimental

import kotlinx.coroutines.experimental.common.AbstractCoroutine
import kotlinx.coroutines.experimental.common.CoroutineScope
import kotlinx.coroutines.experimental.common.Job
import kotlinx.coroutines.experimental.common.newCoroutineContext
import kotlin.browser.window
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.startCoroutine

/**
 * Runs new coroutine and **blocks** current thread _interruptibly_ until its completion.
 * This function should not be used from coroutine. It is designed to bridge regular blocking code
 * to libraries that are written in suspending style, to be used in `main` functions and in tests.
 *
 * The default [CoroutineDispatcher] for this builder in an implementation of [EventLoop] that processes continuations
 * in this blocked thread until the completion of this coroutine.
 * See [CoroutineDispatcher] for the other implementations that are provided by `kotlinx.coroutines`.
 *
 * If this blocked thread is interrupted (see [Thread.interrupt]), then the coroutine job is cancelled and
 * this `runBlocking` invocation throws [InterruptedException].
 *
 * See [newCoroutineContext] for a description of debugging facilities that are available for newly created coroutine.
 *
 * @param context context of the coroutine. The default value is an implementation of [EventLoop].
 * @param block the coroutine code.
 */
public fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T {
    val newContext = newCoroutineContext(context + EmptyCoroutineContext)
    val coroutine = BlockingCoroutine<T>(newContext)
    coroutine.initParentJob(context[Job])
    block.startCoroutine(coroutine, coroutine)
    return coroutine.joinBlocking()
}

private class BlockingCoroutine<T>(
        parentContext: CoroutineContext
) : AbstractCoroutine<T>(parentContext, true) {

    override fun afterCompletion(state: Any?, mode: Int) {
        // wake up blocked thread
        window.releaseEvents()
    }

    @Suppress("UNCHECKED_CAST")
    fun joinBlocking(): T {
        looper(10)
        // now return result
        val state = this.state
        (state as? CompletedExceptionally)?.let { throw it.exception }
        return state as T
    }

    var timer: Int = 0

    fun looper(time: Int) {
        if (time > 0 && !isCompleted) {
            timer = window.setTimeout(looper(time - 1), 1000)
        } else {
            window.clearTimeout(timer)
        }
    }
}