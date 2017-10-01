package reactivity.experimental.javafx

import kotlinx.coroutines.experimental.javafx.JavaFx
import reactivity.experimental.Scheduler
import reactivity.experimental.SchedulerImpl

object SchedulerJavaFx {
    @JvmStatic
    fun javaFxDispatcher(): Scheduler {
        return SchedulerImpl(JavaFx)
    }
}