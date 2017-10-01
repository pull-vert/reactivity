package reactivity.experimental.android

import kotlinx.coroutines.experimental.android.UI
import reactivity.experimental.Scheduler
import reactivity.experimental.SchedulerImpl

object SchedulerAndroid {
    @JvmStatic
    fun uiThreadContext(): Scheduler {
        return SchedulerImpl(UI)
    }
}