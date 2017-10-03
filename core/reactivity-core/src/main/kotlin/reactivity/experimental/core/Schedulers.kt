package reactivity.experimental.core

import kotlinx.coroutines.experimental.*
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

object Schedulers {

    @JvmStatic
    fun defaultDispatcher(): Scheduler {
        return SchedulerImpl(DefaultDispatcher)
    }

    @JvmStatic
    fun emptyContext(): Scheduler {
        return SchedulerImpl(EmptyCoroutineContext)
    }

    @JvmStatic
    fun commonPoolDispatcher(): Scheduler {
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
    fun unconfinedDispatcher(): Scheduler {
        return SchedulerImpl(Unconfined)
    }

    @JvmStatic
    fun fromExecutorDispatcher(exectutor: Executor): Scheduler {
        return SchedulerImpl(exectutor.asCoroutineDispatcher())
    }

    @JvmStatic
    fun fromCoroutineContext(context: CoroutineContext): Scheduler {
        return SchedulerImpl(context)
    }
}

class SchedulerImpl(override val context: CoroutineContext) : Scheduler

interface Scheduler /*: Disposable*/ {
    val context: CoroutineContext
}