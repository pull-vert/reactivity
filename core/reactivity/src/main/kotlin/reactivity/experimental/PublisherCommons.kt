package reactivity.experimental

import kotlinx.coroutines.experimental.reactive.consumeEach

@Suppress("ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE")
actual typealias ProducerScope<E> = kotlinx.coroutines.experimental.channels.ProducerScope<E>

actual public inline suspend fun <T> Publisher<T>.consumeEach(action: (T) -> Unit) = this@consumeEach.consumeEach(action)