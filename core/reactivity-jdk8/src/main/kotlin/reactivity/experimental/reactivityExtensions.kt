package reactivity.experimental

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.reactive.consumeEach
import org.reactivestreams.Publisher
import reactivity.experimental.core.PublisherCommons
import reactivity.experimental.core.Scheduler
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
fun <T> T.toSolo() = Solo.fromValue(this)
fun <T> T.toSolo(scheduler: Scheduler) = Solo.fromValue(scheduler,this)
fun <T> CompletableFuture<T>.toSolo() = Solo.fromCompletableFuture(this)
fun <T> CompletableFuture<T>.toSolo(scheduler: Scheduler) = Solo.fromCompletableFuture(scheduler,this)

// Multi
fun <T> Iterable<T>.toMulti() = Multi.fromIterable(this)
fun <T> Iterable<T>.toMulti(scheduler: Scheduler) = Multi.fromIterable(scheduler, this)
fun BooleanArray.toMulti() = this.toList().toMulti()
fun BooleanArray.toMulti(scheduler: Scheduler) = this.toList().toMulti(scheduler)
fun ByteArray.toMulti() = this.toList().toMulti()
fun ByteArray.toMulti(scheduler: Scheduler) = this.toList().toMulti(scheduler)
fun CharArray.toMulti() = this.toList().toMulti()
fun CharArray.toMulti(scheduler: Scheduler) = this.toList().toMulti(scheduler)
fun DoubleArray.toMulti() = this.toList().toMulti()
fun DoubleArray.toMulti(scheduler: Scheduler) = this.toList().toMulti(scheduler)
fun FloatArray.toMulti() = this.toList().toMulti()
fun FloatArray.toMulti(scheduler: Scheduler) = this.toList().toMulti(scheduler)
fun IntArray.toMulti() = this.toList().toMulti()
fun IntArray.toMulti(scheduler: Scheduler) = this.toList().toMulti(scheduler)
fun LongArray.toMulti() = this.toList().toMulti()
fun LongArray.toMulti(scheduler: Scheduler) = this.toList().toMulti(scheduler)
fun ShortArray.toMulti() = this.toList().toMulti()
fun ShortArray.toMulti(scheduler: Scheduler) = this.toList().toMulti(scheduler)
fun <T> Array<T>.toMulti() = Multi.fromArray(this)
fun <T> Publisher<T>.toMulti() = Multi.fromPublisher(this)
fun <T> Publisher<T>.toMulti(scheduler: Scheduler) = Multi.fromPublisher(scheduler, this)