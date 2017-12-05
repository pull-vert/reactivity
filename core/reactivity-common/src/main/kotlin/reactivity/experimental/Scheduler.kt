package reactivity.experimental

import reactivity.experimental.expect.Scheduler
import kotlin.coroutines.experimental.CoroutineContext

interface WithContext {
    val context: CoroutineContext
}

class SchedulerImpl(override val context: CoroutineContext) : Scheduler()