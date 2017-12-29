package reactivity.experimental

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Unconfined
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext



/**
 * The default scheduler used for instantiation of Multi and Solo
 */
val SCHEDULER_DEFAULT_DISPATCHER: Scheduler = SchedulerImpl(DefaultDispatcher)
val SCHEDULER_EMPTY_CONTEXT: Scheduler = SchedulerImpl(EmptyCoroutineContext)
val SCHEDULER_UNCONFINED_DISPATCHER: Scheduler = SchedulerImpl(Unconfined)
fun schedulerFromCoroutineContext(context: CoroutineContext): Scheduler = SchedulerImpl(context)

class SchedulerImpl(override val context: CoroutineContext) : Scheduler

interface Scheduler /*: DisposableHandle ???*/ {
    val context: CoroutineContext
}