package reactivity.experimental

import kotlinx.coroutines.experimental.AbstractCoroutine
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlin.coroutines.experimental.CoroutineContext

// -------------- Interface definitions

interface Multi<out E> {
    suspend fun consume(sink: Sink<E>)
    companion object Factory
}

interface Sink<in E> {
    suspend fun send(item: E)
    fun close(cause: Throwable?)
}

// cold Multi Coroutine

fun <T> multi(
        context: CoroutineContext = DefaultDispatcher,
        parent: Job? = null,
        block: suspend Sink<T>.() -> Unit
): Multi<T> = object : Multi<T> {
    override suspend fun consume(sink: Sink<T>) {
        val newContext = newCoroutineContext(context, parent)
        val coroutine = MultiCoroutine(newContext, sink)
        coroutine.start(CoroutineStart.DEFAULT, coroutine, block)
    }
}

private class MultiCoroutine<in T>(
        parentContext: CoroutineContext,
        private val sink: Sink<T>
) : AbstractCoroutine<Unit>(parentContext, true), Sink<T> {
    override suspend fun send(item: T) {
        println("sending $item")
        sink.send(item)
    }

    override fun close(cause: Throwable?) {
        sink.close(cause)
    }
}

// -------------- Top level extensions

fun <T> Iterable<T>.toMulti() = object : Multi<T> {
    override suspend fun consume(sink: Sink<T>) {
        var cause: Throwable? = null
        try {
            for (x in this@toMulti) { sink.send(x) }
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
    }
}
fun BooleanArray.toMulti() = this.toList().toMulti()
fun ByteArray.toMulti() = this.toList().toMulti()
fun CharArray.toMulti() = this.toList().toMulti()
fun DoubleArray.toMulti() = this.toList().toMulti()
fun FloatArray.toMulti() = this.toList().toMulti()
fun IntArray.toMulti() = this.toList().toMulti()
fun LongArray.toMulti() = this.toList().toMulti()
fun ShortArray.toMulti() = this.toList().toMulti()
fun <T> Array<T>.toMulti() = this.toList().toMulti()
fun <T> Sequence<T>.toMulti() = object : Multi<T> {
    override suspend fun consume(sink: Sink<T>) {
        var cause: Throwable? = null
        try {
            for (x in this@toMulti) { sink.send(x) }
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
    }
}

// -------------- Factory (initial/producing) operations

fun Multi.Factory.range(start: Int, count: Int) = object : Multi<Int> {
    override suspend fun consume(sink: Sink<Int>) {
        var cause: Throwable? = null
        try {
            for (i in start until (start + count)) { sink.send(i) }
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
    }
}

// -------------- Terminal (final/consuming) operations

suspend inline fun <E, R> Multi<E>.fold(initial: R, crossinline operation: (acc: R, E) -> R): R {
    var acc = initial
    consume(object : Sink<E> {
        override suspend fun send(item: E) {
            acc = operation(acc, item)
        }

        override fun close(cause: Throwable?) { cause?.let { throw it } }
    })
    return acc
}

/**
 * Performs the given [action] for each received element.
 *
 * This function consumes all elements of the original [Multi].
 */
suspend inline fun <E> Multi<E>.consumeEach(crossinline action: (E) -> Unit) =
        consume(object : Sink<E> {
            override suspend fun send(item: E) = action(item)
            override fun close(cause: Throwable?) { cause?.let { throw it } }
        })

// -------------- Intermediate (transforming) operations

fun <E> Multi<E>.delay(time: Int) = object : Multi<E> {
    override suspend fun consume(sink: Sink<E>) {
        var cause: Throwable? = null
        try {
            this@delay.consume(object : Sink<E> {
                override suspend fun send(item: E) {
                    kotlinx.coroutines.experimental.delay(time)
                    sink.send(item)
                }

                override fun close(cause: Throwable?) { cause?.let { throw it } }
            })
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
    }
}

expect fun <E> Multi<E>.filter(predicate: (E) -> Boolean) : Multi<E>

/**
 * Returns a [Multi] that uses the [mapper] to transform each received element from [E]
 * to [F] and then send it when transformation is done
 *
 * @param mapper the mapper function
 */
expect fun <E, F> Multi<E>.map(mapper: (E) -> F) : Multi<F>
