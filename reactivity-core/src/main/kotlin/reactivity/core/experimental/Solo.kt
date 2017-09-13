package reactivity.core.experimental

import org.reactivestreams.Publisher

/** TODO implement the Solo Publisher
 * @see https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.experimental/async.html
 * @see https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.experimental/-completable-deferred/index.html
 */

/**
 * @author Frédéric Montariol
 */
abstract class Solo<T> protected constructor(): Publisher<T> {

}

internal class SoloImpl<T> internal constructor(override final val delegate: Publisher<T>) : Solo<T>(), PublisherDelegated<T> {

}