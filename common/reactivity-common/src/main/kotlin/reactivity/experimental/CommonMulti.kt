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

fun Multi.Factory.range(start: Int, count: Int): Multi<Int> = object : Multi<Int> {
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

expect fun <E> Multi<E>.filter(predicate: (E) -> Boolean) : Multi<E>
