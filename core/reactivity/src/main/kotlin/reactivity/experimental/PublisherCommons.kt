package reactivity.experimental

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.reactive.asPublisher
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.reactive.openSubscription
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.io.use

@Suppress("ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE")
actual typealias ProducerScope<E> = kotlinx.coroutines.experimental.channels.ProducerScope<E>

actual public inline suspend fun <T> Publisher<T>.consumeEach(action: (T) -> Unit) = this@consumeEach.consumeEach(action)

actual internal inline suspend fun <T> CommonPublisher<T>.commonConsumeEach(action: (T) -> Unit) = this@commonConsumeEach.consumeEach(action)

actual typealias Closeable = java.io.Closeable

@Suppress("ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE")
actual typealias ChannelIterator<E> = kotlinx.coroutines.experimental.channels.ChannelIterator<E>

@Suppress("ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE")
actual typealias ReceiveChannel<E> = kotlinx.coroutines.experimental.channels.ReceiveChannel<E>

@Suppress("ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE")
actual typealias SubscriptionReceiveChannel<T> = kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel<T>

actual public fun <T> CommonPublisher<T>.openSubscription(): SubscriptionReceiveChannel<T> = this@openSubscription.openSubscription()

actual public inline fun <T : Closeable?, R> T.use(block: (T) -> R): R = this@use.use(block)

@Suppress("ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE")
actual typealias SendChannel<E> = kotlinx.coroutines.experimental.channels.SendChannel<E>

actual typealias Channel<E> = kotlinx.coroutines.experimental.channels.Channel<E>

actual public fun <E> Channel(capacity: Int): Channel<E> = kotlinx.coroutines.experimental.channels.Channel(capacity)

actual  public fun <T> ReceiveChannel<T>.asPublisher(context: CoroutineContext = DefaultDispatcher): Publisher<T> = this@asPublisher.asPublisher()

actual interface CommonPublisher<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T>, Publisher<T> {
    actual val initialScheduler: Scheduler
}