package reactivity.experimental

import kotlin.coroutines.experimental.CoroutineContext

interface WithContext {
    val context: CoroutineContext
}

class SchedulerImpl(override val context: CoroutineContext) : Scheduler