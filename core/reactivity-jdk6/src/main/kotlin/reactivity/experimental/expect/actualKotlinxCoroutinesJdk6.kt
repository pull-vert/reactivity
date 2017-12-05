//package reactivity.experimental
//
//import kotlinx.coroutines.experimental.CoroutineStart
//import kotlinx.coroutines.experimental.channels.consumeEach
//import kotlin.coroutines.experimental.CoroutineContext
//
//// coroutine contexts
//actual val DefaultDispatcher: CoroutineContext = kotlinx.coroutines.experimental.DefaultDispatcher
//
//// general
//actual typealias CoroutineScope = kotlinx.coroutines.experimental.CoroutineScope
//// FIXME https://github.com/JetBrains/kotlin/blob/master/compiler/frontend/src/org/jetbrains/kotlin/diagnostics/rendering/DefaultErrorMessages.java
//// FIXME MAP.put(ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE, "Aliased class should not have type parameters with declaration-site variance");
//actual typealias ProducerScope<E> = kotlinx.coroutines.experimental.channels.ProducerScope<E>
//actual typealias Job = kotlinx.coroutines.experimental.Job
//actual fun launch(
//        context: CoroutineContext,
//        block: suspend CoroutineScope.() -> Unit
//): Job = kotlinx.coroutines.experimental.launch(context, CoroutineStart.DEFAULT, block)
//
//// selects
//actual typealias SelectClause1<Q> = kotlinx.coroutines.experimental.selects.SelectClause1<Q>
//actual typealias SelectBuilder<R> = kotlinx.coroutines.experimental.selects.SelectBuilder<R>
//actual suspend fun whileSelect(builder: SelectBuilder<Boolean>.() -> Unit)
//        = kotlinx.coroutines.experimental.selects.whileSelect(builder)
//
//// channels
//actual typealias ChannelIterator<E> = kotlinx.coroutines.experimental.channels.ChannelIterator<E>
//actual typealias ReceiveChannel<E> = kotlinx.coroutines.experimental.channels.ReceiveChannel<E>
//actual typealias LinkedListChannel<E> = kotlinx.coroutines.experimental.channels.LinkedListChannel<E>
//actual typealias SubscriptionReceiveChannel<T> = kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel<T>
//actual inline suspend fun <E> ReceiveChannel<E>.consumeEach(action: (E) -> Unit) = this@consumeEach.consumeEach(action)
//actual typealias Channel<E> = kotlinx.coroutines.experimental.channels.Channel<E>
//actual fun <E> Channel(capacity: Int): Channel<E> = kotlinx.coroutines.experimental.channels.Channel(capacity)
