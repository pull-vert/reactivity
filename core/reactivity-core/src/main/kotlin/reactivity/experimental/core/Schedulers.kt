package reactivity.experimental.core

import kotlinx.coroutines.experimental.*
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

// for Kotlin easier call
/**
 * The default scheduler used for instantiation of Multi and Solo
 */
val SECHEDULER_DEFAULT_DISPATCHER: Scheduler = SchedulerImpl(DefaultDispatcher)
val SECHEDULER_EMPTY_CONTEXT: Scheduler = SchedulerImpl(EmptyCoroutineContext)
val SCHEDULER_COMMON_POOL_DISPATCHER: Scheduler = SchedulerImpl(CommonPool)
fun schedulerSingleThreadContext(name: String): Scheduler = SchedulerImpl(newSingleThreadContext(name))
fun schedulerFixedThreadPoolContext(nThreads: Int, name: String): Scheduler = SchedulerImpl(newFixedThreadPoolContext(nThreads, name))
val SCHEDULER_UNCONFINED_DISPATCHER: Scheduler = SchedulerImpl(Unconfined)
fun schedulerFromExecutorDispatcher(exectutor: Executor): Scheduler = SchedulerImpl(exectutor.asCoroutineDispatcher())
fun schedulerFromCoroutineContext(context: CoroutineContext): Scheduler = SchedulerImpl(context)

// for Java static call
object Schedulers {

    @JvmField
    val DEFAULT_DISPATCHER = SECHEDULER_DEFAULT_DISPATCHER

    @JvmField
    val EMPTY_CONTEXT = SECHEDULER_EMPTY_CONTEXT

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

class SchedulerImpl(override val context: CoroutineContext) : Scheduler

interface Scheduler /*: Disposable*/ {
    val context: CoroutineContext
}