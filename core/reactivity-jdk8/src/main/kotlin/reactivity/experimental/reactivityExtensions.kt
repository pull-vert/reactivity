package reactivity.experimental

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.reactive.consumeEach
import org.reactivestreams.Publisher
import reactivity.experimental.core.DEFAULT_SCHEDULER
import reactivity.experimental.core.PublisherCommons
import reactivity.experimental.core.Scheduler

fun <T> PublisherCommons<Publisher<T>>.merge() = merge(initialScheduler)

fun <T> PublisherCommons<Publisher<T>>.merge(scheduler: Scheduler) = multi(scheduler) {
    consumeEach { pub ->                 // for each publisher received on the source channel
        launch(coroutineContext) {       // launch a child coroutine
            pub.consumeEach { send(it) } // resend all element from this publisher
        }
    }
}