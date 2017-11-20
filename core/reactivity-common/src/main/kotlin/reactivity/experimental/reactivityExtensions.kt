package reactivity.experimental

import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

// for Kotlin easier call, top level functions

// Scheduler
/**
 * The default scheduler used for instantiation of Multi and Solo
 */
val SCHEDULER_DEFAULT_DISPATCHER : Scheduler = SchedulerImpl(DefaultDispatcher)
val SCHEDULER_EMPTY_CONTEXT : Scheduler = SchedulerImpl(EmptyCoroutineContext)
fun schedulerFromCoroutineContext(context: CoroutineContext) : Scheduler = SchedulerImpl(context)

// Solo
fun <T> T.toSolo(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = solo(scheduler) {
    send(this@toSolo)
}

// Multi
fun <T> Iterable<T>.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = multi(scheduler) {
    for (x in this@toMulti) send(x)
}
fun BooleanArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun ByteArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun CharArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun DoubleArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun FloatArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun IntArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun LongArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun ShortArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun <T> Array<T>.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = multi(scheduler) {
    for (x in this@toMulti) send(x)
}

// TODO : Junit to test that !!
fun <T> PublisherCommons<Publisher<T>>.merge() = multi(initialScheduler) {
    consumeEach { pub ->                 // for each publisher received on the source channel
        launch(context) {       // launch a child coroutine
            pub.consumeEach { send(it) } // resend all element from this publisher
        }
    }
}