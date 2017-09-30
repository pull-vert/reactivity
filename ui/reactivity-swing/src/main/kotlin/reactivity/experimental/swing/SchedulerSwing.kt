package reactivity.experimental.swing

import kotlinx.coroutines.experimental.swing.Swing
import reactivity.experimental.Scheduler
import reactivity.experimental.SchedulerImpl

object SchedulerSwing {
    @JvmStatic
    fun swingThreadContext(): Scheduler {
        return SchedulerImpl(Swing)
    }
}