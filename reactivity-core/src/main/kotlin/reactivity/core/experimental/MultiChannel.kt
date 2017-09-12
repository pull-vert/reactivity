package reactivity.core.experimental

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.reactive.asPublisher
import kotlin.coroutines.experimental.CoroutineContext

internal class MultiChannel<T> internal constructor(coroutineContext: CoroutineContext, val channel: Channel<T>) : MultiImpl<T>(channel.asPublisher(coroutineContext)) {
}