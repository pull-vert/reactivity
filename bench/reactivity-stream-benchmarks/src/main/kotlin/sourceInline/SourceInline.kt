package sourceInline

import kotlinx.coroutines.experimental.runBlocking

// -------------- Interface definitions

interface SourceInline<out E> {
    fun consume(sink: Sink<E>)
    companion object Factory
}

interface Sink<in E> {
    suspend fun send(item: E)
    fun close(cause: Throwable?)
}

// -------------- Factory (initial/producing) operations

fun SourceInline.Factory.range(start: Int, count: Int): SourceInline<Int> = object : SourceInline<Int> {
    override fun consume(sink: Sink<Int>) {
        var cause: Throwable? = null
        try {
            runBlocking {
                for (i in start until (start + count)) {
                    sink.send(i)
                }
            }
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
    }
}

// -------------- Terminal (final/consuming) operations

inline fun <E, R> SourceInline<E>.fold(initial: R, crossinline operation: (acc: R, E) -> R): R {
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

inline fun <E> SourceInline<E>.filter(crossinline predicate: (E) -> Boolean) = object : SourceInline<E> {
    override fun consume(sink: Sink<E>) {
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