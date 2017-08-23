package reactivity

import kotlinx.coroutines.experimental.reactive.publish
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

class Multi<T> constructor(val delegate: Publisher<T>) : Publisher<T> {
    override fun subscribe(s: Subscriber<in T>?) {
        delegate.subscribe(s)
    }
}

fun <T> Publisher<T>.toMulti(): Multi<T> = Multi(this)

fun multiFromRange(start: Int, count: Int, context: CoroutineContext = EmptyCoroutineContext): Multi<Int> = publish(context) {
    for (x in start until start + count) send(x)
}.toMulti()