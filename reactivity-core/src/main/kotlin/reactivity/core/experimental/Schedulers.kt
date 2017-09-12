package reactivity.core.experimental

import kotlinx.coroutines.experimental.*
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

object Schedulers {

    @JvmStatic
    fun emptyThreadContext(): Scheduler {
        return SchedulerImpl(EmptyCoroutineContext)
    }

    @JvmStatic
    fun commonPoolThreadContext(): Scheduler {
        return SchedulerImpl(CommonPool)
    }

    @JvmStatic
    fun singleThreadContext(): Scheduler {
        return SchedulerImpl(newSingleThreadContext("singleThread"))
    }

    @JvmStatic
    fun fixedThreadPoolContext(nThreads: Int): Scheduler {
        return SchedulerImpl(newFixedThreadPoolContext(nThreads, "fixedThread"))
    }

    @JvmStatic
    fun unconfinedThreadContext(): Scheduler {
        return SchedulerImpl(Unconfined)
    }

    @JvmStatic
    fun fromExecutor(exectutor: Executor): Scheduler {
        return SchedulerImpl(exectutor.asCoroutineDispatcher())
    }

    @JvmStatic
    fun fromCoroutineContext(context: CoroutineContext): Scheduler {
        return SchedulerImpl(context)
    }

    private class SchedulerImpl(override val context: CoroutineContext) : Scheduler
}

interface Scheduler /*: Disposable*/ {
    val context: CoroutineContext
}