package reactivity.experimental.core

import kotlinx.coroutines.experimental.*
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

// for Kotlin easiest call
val SECHEDULER_EMPTY_CONTEXT: Scheduler = SchedulerImpl(EmptyCoroutineContext)

// for Java static call
object Schedulers {

    @JvmStatic
    fun defaultDispatcher(): Scheduler = SchedulerImpl(DefaultDispatcher)

    @JvmStatic
    fun emptyContext() = SECHEDULER_EMPTY_CONTEXT

    @JvmStatic
    fun commonPoolDispatcher(): Scheduler = SchedulerImpl(CommonPool)

    @JvmStatic
    fun singleThreadContext(): Scheduler = SchedulerImpl(newSingleThreadContext("singleThread"))

    @JvmStatic
    fun fixedThreadPoolContext(nThreads: Int): Scheduler = SchedulerImpl(newFixedThreadPoolContext(nThreads, "fixedThread"))

    @JvmStatic
    fun unconfinedDispatcher(): Scheduler = SchedulerImpl(Unconfined)

    @JvmStatic
    fun fromExecutorDispatcher(exectutor: Executor): Scheduler = SchedulerImpl(exectutor.asCoroutineDispatcher())

    @JvmStatic
    fun fromCoroutineContext(context: CoroutineContext): Scheduler = SchedulerImpl(context)
}

class SchedulerImpl(override val context: CoroutineContext) : Scheduler

interface Scheduler /*: Disposable*/ {
    val context: CoroutineContext
}