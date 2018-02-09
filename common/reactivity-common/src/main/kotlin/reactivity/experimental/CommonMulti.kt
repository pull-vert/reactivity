package reactivity.experimental

// -------------- Interface definitions

interface Multi<out E> {
    suspend fun consume(sink: Sink<E>)
    companion object Factory
}

interface Sink<in E> {
    suspend fun send(item: E)
    fun close(cause: Throwable?)
}

// -------------- Top level extensions

fun <T> Iterable<T>.toMulti() = object : Multi<T> {
    suspend override fun consume(sink: Sink<T>) {
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
    suspend override fun consume(sink: Sink<T>) {
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
    suspend override fun consume(sink: Sink<Int>) {
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

inline suspend fun <E, R> Multi<E>.fold(initial: R, crossinline operation: (acc: R, E) -> R): R {
    var acc = initial
    consume(object : Sink<E> {
        suspend override fun send(item: E) {
            acc = operation(acc, item)
        }

        override fun close(cause: Throwable?) { cause?.let { throw it } }
    })
    return acc
}

/**
 * Performs the given [action] for each received element.
 *
 * This function [consumes][consume] all elements of the original [Multi].
 */
inline suspend fun <E> Multi<E>.consumeEach(crossinline action: (E) -> Unit) =
        consume(object : Sink<E> {
            suspend override fun send(item: E) = action(item)
            override fun close(cause: Throwable?) { cause?.let { throw it } }
        })

// -------------- Intermediate (transforming) operations

fun <E> Multi<E>.delay(time: Int) = object : Multi<E> {
    suspend override fun consume(sink: Sink<E>) {
        var cause: Throwable? = null
        try {
            this@delay.consume(object : Sink<E> {
                suspend override fun send(item: E) {
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
