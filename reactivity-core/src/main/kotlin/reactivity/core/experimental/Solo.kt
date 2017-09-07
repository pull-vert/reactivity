package reactivity.core.experimental

import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.reactive.publish
import org.reactivestreams.Publisher
import kotlin.coroutines.experimental.CoroutineContext

fun <T> solo(
        context: CoroutineContext,
        block: suspend ProducerScope<T>.() -> Unit
): Solo<T> = SoloImpl(publish(context, block))

abstract class Solo<T> : Publisher<T> {

}

internal class SoloImpl<T> internal constructor(override val delegate: Publisher<T>) : Solo<T>(), PublisherDelegated<T> {

}