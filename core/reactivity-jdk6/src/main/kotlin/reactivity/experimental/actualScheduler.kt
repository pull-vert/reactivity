package reactivity.experimental

import java.util.concurrent.Executor
import kotlin.coroutines.experimental.CoroutineContext

actual abstract class Scheduler : WithContext {
    // for Java static call
    companion object Schedulers {

        @JvmField
        val DEFAULT_DISPATCHER = SCHEDULER_DEFAULT_DISPATCHER

        @JvmField
        val EMPTY_CONTEXT = SCHEDULER_EMPTY_CONTEXT

        @JvmField
        val COMMON_POOL_DISPATCHER = SCHEDULER_COMMON_POOL_DISPATCHER

        @JvmStatic
        fun singleThreadContext(name: String) = schedulerSingleThreadContext(name)

        @JvmStatic
        fun fixedThreadPoolContext(nThreads: Int, name: String) = schedulerFixedThreadPoolContext(nThreads, name)

        @JvmField
        val UNCONFINED_DISPATCHER = SCHEDULER_UNCONFINED_DISPATCHER

        @JvmStatic
        fun fromExecutorDispatcher(exectutor: Executor) = schedulerFromExecutorDispatcher(exectutor)

        @JvmStatic
        fun fromCoroutineContext(context: CoroutineContext) = schedulerFromCoroutineContext(context)
    }
}