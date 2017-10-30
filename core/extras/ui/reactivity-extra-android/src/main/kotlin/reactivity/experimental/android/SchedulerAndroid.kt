package reactivity.experimental.android

import kotlinx.coroutines.experimental.android.UI
import reactivity.experimental.Scheduler
import reactivity.experimental.SchedulerImpl

// for Kotlin easier call
val SCHEDULER_ANDROID_DISPATCHER: Scheduler = SchedulerImpl(UI)

// for Java static call
object SchedulerAndroid {
    @JvmStatic
    fun androidDispatcher() = SCHEDULER_ANDROID_DISPATCHER
}