package reactivity.experimental

import kotlinx.coroutines.experimental.launch

// TODO : Junit to test that !!
actual fun <T> Publisher<Publisher<T>>.merge(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = multi(scheduler) {
    consumeEach { pub ->                 // for each publisher received on the source channel
        launch(coroutineContext) {       // launch a child coroutine
            pub.consumeEach { send(it) } // resend all element from this publisher
        }
    }
}

// Multi
actual fun <T> Publisher<T>.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER, key: Multi.Key<*>? = null): Multi<T> = MultiImpl(this@toMulti, scheduler, key)