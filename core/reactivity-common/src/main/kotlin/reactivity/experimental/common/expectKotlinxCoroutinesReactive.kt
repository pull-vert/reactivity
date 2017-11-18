package reactivity.experimental.common

import kotlin.coroutines.experimental.CoroutineContext

expect inline suspend fun <T> Publisher<T>.consumeEach(action: (T) -> Unit)
expect fun <T> publish(context: CoroutineContext, block: suspend ProducerScope<T>.() -> Unit): Publisher<T>
expect fun <T> Publisher<T>.openSubscription(): SubscriptionReceiveChannel<T>
expect suspend fun <T> Publisher<T>.awaitSingle(): T
expect fun <T> ReceiveChannel<T>.asPublisher(context: CoroutineContext): Publisher<T>