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

// -------------- Factory (initial/producing) operations

fun Multi.Factory.range(start: Int, count: Int) = object : Multi<Int> {
    suspend override fun consume(sink: Sink<Int>) {
        var cause: Throwable? = null
        try {
            for (i in start until (start + count)) {
                sink.send(i)
            }
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

        override fun close(cause: Throwable?) {
            cause?.let { throw it }
        }
    })
    return acc
}

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

                override fun close(cause: Throwable?) {
                    cause?.let { throw it }
                }
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
