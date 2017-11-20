package reactivity.experimental

import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlin.coroutines.experimental.CoroutineContext

// coroutine contexts
actual val DefaultDispatcher: CoroutineContext = kotlinx.coroutines.experimental.DefaultDispatcher

// general
actual typealias CoroutineScope = kotlinx.coroutines.experimental.CoroutineScope
// FIXME remove this added Interface when declarative-site variance will be supported for alias
interface MyProducerScope<E> : kotlinx.coroutines.experimental.channels.ProducerScope<E>
actual typealias ProducerScope<E> = MyProducerScope<E>
actual typealias Job = kotlinx.coroutines.experimental.Job
actual fun launch(
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> Unit
): Job = kotlinx.coroutines.experimental.launch(context, CoroutineStart.DEFAULT, block)

// selects
// remove this added Interface when declarative-site variance will be supported for alias
//interface MySelectClause1<Q> : kotlinx.coroutines.experimental.selects.SelectClause1<Q>
actual typealias SelectClause1<Q> = kotlinx.coroutines.experimental.selects.SelectClause1<Q>
// FIXME remove this added Interface when declarative-site variance will be supported for alias
interface MySelectBuilder<R> : kotlinx.coroutines.experimental.selects.SelectBuilder<R> {
    operator fun <Q> SelectClause1<Q>.invoke(block: suspend (Q) -> R)
}
actual typealias SelectBuilder<R> = MySelectBuilder<R>
actual suspend fun whileSelect(builder: SelectBuilder<Boolean>.() -> Unit)
        = kotlinx.coroutines.experimental.selects.whileSelect(
        builder as kotlinx.coroutines.experimental.selects.SelectBuilder<Boolean>.() -> Unit)

// channels
// FIXME remove this added Interface when declarative-site variance will be supported for alias
//interface MyChannelIterator<E> : kotlinx.coroutines.experimental.channels.ChannelIterator<E>
actual typealias ChannelIterator<E> = kotlinx.coroutines.experimental.channels.ChannelIterator<E>
//actual interface ChannelIterator<E> {
//    actual suspend operator fun hasNext(): Boolean
//    actual suspend operator fun next(): E
//}
// FIXME remove this added Interface when declarative-site variance will be supported for alias
//interface MyReceiveChannel<E> : kotlinx.coroutines.experimental.channels.ReceiveChannel<E>// {
//    override operator fun iterator(): ChannelIterator<E>
//    override val onReceive: SelectClause1<E>
//}
actual typealias ReceiveChannel<E> = kotlinx.coroutines.experimental.channels.ReceiveChannel<E>
open class MyLinkedListChannel<E> : kotlinx.coroutines.experimental.channels.LinkedListChannel<E>()//, ReceiveChannel<E>

actual typealias LinkedListChannel<E> = kotlinx.coroutines.experimental.channels.LinkedListChannel<E>
actual typealias SubscriptionReceiveChannel<T> = SubscriptionReceiveChannel<T>

expect inline suspend fun <E> ReceiveChannel<E>.consumeEach(action: (E) -> Unit)
expect interface Channel<E> : ReceiveChannel<E>, SendChannel<E>

expect fun <E> Channel(capacity: Int): Channel<E>