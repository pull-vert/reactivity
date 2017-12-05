package kotlinx.coroutines.experimental

import kotlinx.coroutines.experimental.common.AbstractCoroutine
import kotlinx.coroutines.experimental.common.CoroutineScope
import kotlinx.coroutines.experimental.common.Job
import kotlinx.coroutines.experimental.common.newCoroutineContext
import java.util.concurrent.locks.LockSupport
import kotlin.coroutines.experimental.ContinuationInterceptor
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
@Throws(InterruptedException::class)
public fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T {
    val currentThread = Thread.currentThread()
    val eventLoop = if (context[ContinuationInterceptor] == null) BlockingEventLoop(currentThread) else null
    val newContext = newCoroutineContext(context + (eventLoop ?: EmptyCoroutineContext))
    val coroutine = BlockingCoroutine<T>(newContext, currentThread, privateEventLoop = eventLoop != null)
    coroutine.initParentJob(context[Job])
    block.startCoroutine(coroutine, coroutine)
    return coroutine.joinBlocking()
}

private class BlockingCoroutine<T>(
        parentContext: CoroutineContext,
        private val blockedThread: Thread,
        private val privateEventLoop: Boolean
) : AbstractCoroutine<T>(parentContext, true) {
    private val eventLoop: EventLoop? = parentContext[ContinuationInterceptor] as? EventLoop

    init {
        if (privateEventLoop) require(eventLoop is BlockingEventLoop)
    }

    override fun afterCompletion(state: Any?, mode: Int) {
        // wake up blocked thread
        if (Thread.currentThread() != blockedThread)
            LockSupport.unpark(blockedThread)
    }

    @Suppress("UNCHECKED_CAST")
    fun joinBlocking(): T {
        timeSource.registerTimeLoopThread()
        while (true) {
            if (Thread.interrupted()) throw InterruptedException().also { cancel(it) }
            val parkNanos = eventLoop?.processNextEvent() ?: Long.MAX_VALUE
            // note: process next even may loose unpark flag, so check if completed before parking
            if (isCompleted) break
            timeSource.parkNanos(this, parkNanos)
        }
        // process queued events (that could have been added after last processNextEvent and before cancel
        if (privateEventLoop) (eventLoop as BlockingEventLoop).apply {
            // We exit the "while" loop above when this coroutine's state "isCompleted",
            // Here we should signal that BlockingEventLoop should not accept any more tasks
            isCompleted = true
            shutdown()
        }
        timeSource.unregisterTimeLoopThread()
        // now return result
        val state = this.state
        (state as? CompletedExceptionally)?.let { throw it.exception }
        return state as T
    }
}