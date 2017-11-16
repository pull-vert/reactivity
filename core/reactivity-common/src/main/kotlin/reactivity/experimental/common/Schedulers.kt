package reactivity.experimental.common

import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

// for Kotlin easier call, top level functions
/**
 * The default scheduler used for instantiation of Multi and Solo
 */
val SCHEDULER_DEFAULT_DISPATCHER: Scheduler = SchedulerImpl(DefaultDispatcher)
val SCHEDULER_EMPTY_CONTEXT: Scheduler = SchedulerImpl(EmptyCoroutineContext)
fun schedulerFromCoroutineContext(context: CoroutineContext): Scheduler = SchedulerImpl(context)

class SchedulerImpl(override val context: CoroutineContext) : Scheduler

interface Scheduler /*: Disposable*/ {
    val context: CoroutineContext
}