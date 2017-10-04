package reactivity.experimental.core

import kotlinx.coroutines.experimental.*
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

// for Kotlin easier call
val SECHEDULER_DEFAULT_DISPATCHER: Scheduler = SchedulerImpl(DefaultDispatcher)
val SECHEDULER_EMPTY_CONTEXT: Scheduler = SchedulerImpl(EmptyCoroutineContext)
val SCHEDULER_COMMON_POOL_DISPATCHER: Scheduler = SchedulerImpl(CommonPool)
val SCHEDULER_SINGLE_THREAD_CONTEXT: Scheduler = SchedulerImpl(newSingleThreadContext("singleThread"))
fun schedulerFixedThreadPoolContext(nThreads: Int): Scheduler = SchedulerImpl(newFixedThreadPoolContext(nThreads, "fixedThread"))
val SCHEDULER_UNCONFINED_DISPATCHER: Scheduler = SchedulerImpl(Unconfined)
fun schedulerFromExecutorDispatcher(exectutor: Executor): Scheduler = SchedulerImpl(exectutor.asCoroutineDispatcher())
fun schedulerFromCoroutineContext(context: CoroutineContext): Scheduler = SchedulerImpl(context)

// for Java static call
object Schedulers {

    @JvmStatic
    fun defaultDispatcher() = SECHEDULER_DEFAULT_DISPATCHER

    @JvmStatic
    fun emptyContext() = SECHEDULER_EMPTY_CONTEXT

    @JvmStatic
    fun commonPoolDispatcher() = SCHEDULER_COMMON_POOL_DISPATCHER

    @JvmStatic
    fun singleThreadContext() = SCHEDULER_SINGLE_THREAD_CONTEXT

    @JvmStatic
    fun fixedThreadPoolContext(nThreads: Int) = schedulerFixedThreadPoolContext(nThreads)

    @JvmStatic
    fun unconfinedDispatcher() = SCHEDULER_UNCONFINED_DISPATCHER

    @JvmStatic
    fun fromExecutorDispatcher(exectutor: Executor) = schedulerFromExecutorDispatcher(exectutor)

    @JvmStatic
    fun fromCoroutineContext(context: CoroutineContext) = schedulerFromCoroutineContext(context)
}

class SchedulerImpl(override val context: CoroutineContext) : Scheduler

interface Scheduler /*: Disposable*/ {
    val context: CoroutineContext
}