package reactivity.experimental

import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

abstract class Schedulers private constructor() {

    companion object {
        @JvmStatic fun emptyCoroutineContext(): Scheduler {
            return SchedulerImpl(EmptyCoroutineContext)
        }

        private class SchedulerImpl (override val context: CoroutineContext) : Scheduler
    }


}

interface Scheduler /*: Disposable*/ {
   val context: CoroutineContext
}