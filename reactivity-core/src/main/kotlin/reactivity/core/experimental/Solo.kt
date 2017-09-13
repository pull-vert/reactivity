package reactivity.core.experimental

import kotlinx.coroutines.experimental.Deferred
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

/** TODO implement the Solo Publisher
 * @see https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.experimental/async.html
 * @see https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.experimental/-completable-deferred/index.html
 */

/**
 * @author Frédéric Montariol
 */
abstract class Solo<T> : Publisher<T> {

}

internal class SoloImpl<T> internal constructor(val block: (Subscriber<T>) -> Deferred<T?>) : Solo<T>() {
    lateinit var value: Deferred<T?>

    override fun subscribe(s: Subscriber<in T>) {
        value = block(s as Subscriber<T>)
    }
}