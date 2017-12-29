package reactivity.experimental

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.CoroutineContext

// for Kotlin easier call
val SCHEDULER_COMMON_POOL_DISPATCHER: Scheduler = SchedulerImpl(CommonPool)
fun schedulerSingleThreadContext(name: String): Scheduler = SchedulerImpl(newSingleThreadContext(name))
fun schedulerFixedThreadPoolContext(nThreads: Int, name: String): Scheduler = SchedulerImpl(newFixedThreadPoolContext(nThreads, name))
fun schedulerFromExecutorDispatcher(exectutor: Executor): Scheduler = SchedulerImpl(exectutor.asCoroutineDispatcher())

// for Java static call
object Schedulers {

    @JvmField
    val DEFAULT_DISPATCHER = SCHEDULER_DEFAULT_DISPATCHER

    @JvmField
    val EMPTY_CONTEXT = SCHEDULER_EMPTY_CONTEXT

    @JvmField
    val COMMON_POOL_DISPATCHER = SCHEDULER_COMMON_POOL_DISPATCHER

    @JvmStatic
    fun singleThreadContext(name: String) = schedulerSingleThreadContext(name)

    @JvmStatic
    fun fixedThreadPoolContext(nThreads: Int, name: String) = schedulerFixedThreadPoolContext(nThreads, name)

    @JvmField
    val UNCONFINED_DISPATCHER = SCHEDULER_UNCONFINED_DISPATCHER

    @JvmStatic
    fun fromExecutorDispatcher(exectutor: Executor) = schedulerFromExecutorDispatcher(exectutor)

    @JvmStatic
    fun fromCoroutineContext(context: CoroutineContext) = schedulerFromCoroutineContext(context)
}