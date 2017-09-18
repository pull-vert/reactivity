package reactivity.core.experimental

import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.reactive.publish
import org.reactivestreams.Publisher
import kotlin.coroutines.experimental.CoroutineContext

internal fun <T, R> multiGrouped(
        context: CoroutineContext,
        key: R,
        block: suspend ProducerScope<T>.() -> Unit
): MultiGrouped<T, R> = MultiGroupedImpl(publish(context, block), key)

interface MultiGrouped<T, R> : Multi<T> {
    val key : R
}

internal class MultiGroupedImpl<T, R> internal constructor(delegated: Publisher<T>, override val key: R)
    : MultiImpl<T>(delegated), MultiGrouped<T, R>

