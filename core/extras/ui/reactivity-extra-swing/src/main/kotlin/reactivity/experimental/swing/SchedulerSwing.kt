package reactivity.experimental.swing

import kotlinx.coroutines.experimental.swing.Swing
import reactivity.experimental.core.Scheduler
import reactivity.experimental.core.SchedulerImpl

// for Kotlin easier call
val SCHEDULER_SWING_DISPATCHER: Scheduler = SchedulerImpl(Swing)

// for Java static call
object SchedulerSwing {
    @JvmStatic
    fun swingDispatcher() = SCHEDULER_SWING_DISPATCHER
}