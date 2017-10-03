package reactivity.experimental.android

import kotlinx.coroutines.experimental.android.UI
import reactivity.experimental.core.Scheduler
import reactivity.experimental.core.SchedulerImpl

object SchedulerAndroid {
    @JvmStatic
    fun uiThreadContext(): Scheduler {
        return SchedulerImpl(UI)
    }
}