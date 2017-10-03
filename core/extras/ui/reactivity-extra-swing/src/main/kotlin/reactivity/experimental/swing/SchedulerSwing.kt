package reactivity.experimental.swing

import kotlinx.coroutines.experimental.swing.Swing
import reactivity.experimental.core.Scheduler
import reactivity.experimental.core.SchedulerImpl

object SchedulerSwing {
    @JvmStatic
    fun swingDispatcher(): Scheduler {
        return SchedulerImpl(Swing)
    }
}