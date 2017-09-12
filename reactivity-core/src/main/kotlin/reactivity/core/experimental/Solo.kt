package reactivity.core.experimental

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.reactive.publish
import org.reactivestreams.Publisher
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

/** TODO implement the Solo Publisher
 * @see https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.experimental/async.html
 * @see https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.experimental/-completable-deferred/index.html
 */
fun <T> solo(
        context: CoroutineContext,
        block: suspend ProducerScope<T>.() -> Unit
): Solo<T> = SoloImpl(publish(context, block))

abstract class Solo<T> : Publisher<T> {
 fun asyncFun() {
     async(EmptyCoroutineContext) {

     }
 }
}

internal class SoloImpl<T> internal constructor(override val delegate: Publisher<T>) : Solo<T>(), PublisherDelegated<T> {

}