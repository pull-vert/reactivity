package reactivity.experimental

import kotlinx.coroutines.experimental.*
import reactivity.experimental.expect.Scheduler
import java.util.concurrent.Executor

// for Kotlin easier call, top level functions

// Scheduler
/**
 * The default scheduler used for instantiation of Multi and Solo
 */
val SCHEDULER_COMMON_POOL_DISPATCHER : Scheduler = SchedulerImpl(CommonPool)
fun schedulerSingleThreadContext(name: String) : Scheduler = SchedulerImpl(newSingleThreadContext(name))
fun schedulerFixedThreadPoolContext(nThreads: Int, name: String) : Scheduler = SchedulerImpl(newFixedThreadPoolContext(nThreads, name))
val SCHEDULER_UNCONFINED_DISPATCHER : Scheduler = SchedulerImpl(Unconfined)
fun schedulerFromExecutorDispatcher(exectutor: Executor) : Scheduler = SchedulerImpl(exectutor.asCoroutineDispatcher())