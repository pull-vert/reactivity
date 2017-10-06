package reactivity.experimental

import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.reactive.consumeEach
import org.reactivestreams.Publisher
import reactivity.experimental.core.*
import java.util.concurrent.CompletableFuture

fun <T> PublisherCommons<Publisher<T>>.merge() = merge(initialScheduler)

// TODO : Junit to test that !!
fun <T> PublisherCommons<Publisher<T>>.merge(scheduler: Scheduler) = multi(scheduler) {
    consumeEach { pub ->                 // for each publisher received on the source channel
        launch(coroutineContext) {       // launch a child coroutine
            pub.consumeEach { send(it) } // resend all element from this publisher
        }
    }
}

// Solo
fun <T> T.toSolo(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER): Solo<T> = SoloImpl(DefaultSoloFactory.fromValue(scheduler, this))
fun <T> CompletableFuture<T>.toSolo(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = solo(scheduler) {
    send(this@toSolo.await())
}

// Multi
fun <T> Iterable<T>.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER): Multi<T> = MultiImpl(DefaultMultiFactory.fromIterable(scheduler, this))
fun BooleanArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun ByteArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun CharArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun DoubleArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun FloatArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun IntArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun LongArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun ShortArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun <T> Array<T>.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER): Multi<T> = MultiImpl(DefaultMultiFactory.fromArray(scheduler, this))
fun <T> Publisher<T>.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER): Multi<T> = MultiImpl(DefaultMultiFactory.fromPublisher(scheduler, this))