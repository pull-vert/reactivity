package reactivity.experimental

// -------------- Intermediate (transforming) operations

actual fun <E> Multi<E>.filter(predicate: (E) -> Boolean) = object : Multi<E> {
    suspend override fun consume(sink: Sink<E>) {
        var cause: Throwable? = null
        try {
            this@filter.consume(object : Sink<E> {
                suspend override fun send(item: E) {
                    if (predicate(item)) sink.send(item)
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

actual fun <E, F> Multi<E>.map(mapper: (E) -> F) = object : Multi<F> {
    suspend override fun consume(sink: Sink<F>) {
        var cause: Throwable? = null
        try {
            this@map.consume(object : Sink<E> {
                suspend override fun send(item: E) {
                    sink.send(mapper(item))
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

