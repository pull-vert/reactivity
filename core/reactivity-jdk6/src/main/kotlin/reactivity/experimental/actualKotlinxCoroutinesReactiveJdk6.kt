//package reactivity.experimental
//
//import kotlinx.coroutines.experimental.reactive.asPublisher
//import kotlinx.coroutines.experimental.reactive.awaitSingle
//import kotlinx.coroutines.experimental.reactive.consumeEach
//import kotlinx.coroutines.experimental.reactive.openSubscription
//import kotlin.coroutines.experimental.CoroutineContext
//
//actual inline suspend fun <T> Publisher<T>.consumeEach(action: (T) -> Unit) = this@consumeEach.consumeEach(action)
//actual fun <T> publish(context: CoroutineContext, block: suspend ProducerScope<T>.() -> Unit): Publisher<T> = kotlinx.coroutines.experimental.reactive.publish(context, block)
//actual fun <T> Publisher<T>.openSubscription(): SubscriptionReceiveChannel<T> = this@openSubscription.openSubscription()
//actual suspend fun <T> Publisher<T>.awaitSingle(): T = this@awaitSingle.awaitSingle()
//actual fun <T> ReceiveChannel<T>.asPublisher(context: CoroutineContext): Publisher<T> = this@asPublisher.asPublisher(context)