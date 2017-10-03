package reactivity.experimental.javafx

import kotlinx.coroutines.experimental.javafx.JavaFx
import reactivity.experimental.core.Scheduler
import reactivity.experimental.core.SchedulerImpl

object SchedulerJavaFx {
    @JvmStatic
    fun javaFxDispatcher(): Scheduler {
        return SchedulerImpl(JavaFx)
    }
}