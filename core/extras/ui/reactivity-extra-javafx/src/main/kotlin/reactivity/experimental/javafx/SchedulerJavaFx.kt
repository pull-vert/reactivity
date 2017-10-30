package reactivity.experimental.javafx

import kotlinx.coroutines.experimental.javafx.JavaFx
import reactivity.experimental.Scheduler
import reactivity.experimental.SchedulerImpl

// for Kotlin easier call
val SCHEDULER_JAVA_FX_DISPATCHER: Scheduler = SchedulerImpl(JavaFx)

// for Java static call
object SchedulerJavaFx {
    @JvmStatic
    fun javaFxDispatcher() = SCHEDULER_JAVA_FX_DISPATCHER
}