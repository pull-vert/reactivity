package reactivity.experimental.jdk8

import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import kotlinx.coroutines.experimental.reactive.awaitSingle
import reactivity.experimental.*
import java.util.concurrent.CompletableFuture
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

// Solo
fun <T> CompletableFuture<T>.toSolo(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = solo(scheduler) {
    send(this@toSolo.await())
}

fun <T> Solo<T>.toCompletableFuture(scheduler: Scheduler = initialScheduler) = future(scheduler.context) {
    awaitSingle()
}

// Multi
fun <T> Stream<T>.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toArray().toMulti(scheduler)
fun IntStream.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toArray().toMulti(scheduler)
fun LongStream.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toArray().toMulti(scheduler)
fun DoubleStream.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toArray().toMulti(scheduler)