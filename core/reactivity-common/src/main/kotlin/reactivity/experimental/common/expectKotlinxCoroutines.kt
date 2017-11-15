package reactivity.experimental.common

import kotlin.coroutines.experimental.CoroutineContext

// coroutine contexts
expect abstract class CoroutineDispatcher: CoroutineContext
expect class ThreadPoolDispatcher: CoroutineContext

expect val DefaultDispatcher: CoroutineDispatcher
expect object CommonPool : CoroutineDispatcher
expect fun newSingleThreadContext(name: String): ThreadPoolDispatcher
expect fun newFixedThreadPoolContext(nThreads: Int, name: String): ThreadPoolDispatcher
expect object Unconfined : CoroutineDispatcher
expect fun Executor.asCoroutineDispatcher(): CoroutineDispatcher