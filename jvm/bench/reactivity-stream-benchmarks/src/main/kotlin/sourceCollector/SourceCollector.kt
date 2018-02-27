package sourceCollector

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import reactivity.experimental.channel.Element
import reactivity.experimental.channel.SpScChannel
import kotlin.coroutines.experimental.CoroutineContext

// -------------- Model definitions

interface SourceCollector<out E> {
    suspend fun <T> consume(sink: Sink<E>, collector: (() -> T)? = null): T?
    companion object Factory
}

interface Sink<in E> {
    suspend fun send(item: E)
    fun close(cause: Throwable?)
}

// -------------- Factory (initial/producing) operations

fun SourceCollector.Factory.range(start: Int, count: Int): SourceCollector<Int> = object : SourceCollector<Int> {
    override suspend fun <T> consume(sink: Sink<Int>, collector: (() -> T)?): T? {
        var cause: Throwable? = null
        try {
            for (i in start until (start + count)) {
                sink.send(i)
            }
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
        return collector?.invoke()
    }
}

// -------------- Terminal (final/consuming) operations

inline suspend fun <E, R> SourceCollector<E>.fold(initial: R, crossinline operation: (acc: R, E) -> R): R {
    var acc = initial
    return consume(object : Sink<E> {
        override suspend fun send(item: E) {
            acc = operation(acc, item)
        }

        override fun close(cause: Throwable?) {
            cause?.let { throw it }
        }
    }, collector = {
        return@consume acc
    })!!
}

// -------------- Intermediate (transforming) operations

inline fun <E> SourceCollector<E>.filter(crossinline predicate: (E) -> Boolean) = object : SourceCollector<E> {
    override suspend fun <T> consume(sink: Sink<E>, collector: (() -> T)?): T? {
        var cause: Throwable? = null
        try {
            this@filter.consume<Unit>(object : Sink<E> {
                override suspend fun send(item: E) {
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
        return collector?.invoke()
    }
}



fun <E : Any> SourceCollector<E>.async(context: CoroutineContext = DefaultDispatcher, buffer: Int = 0): SourceCollector<E> {
    val channel = SpScChannel<E>(buffer)
    return object : SourceCollector<E> {
        override suspend fun <T> consume(sink: Sink<E>, collector: (() -> T)?): T? {
            // Get return value of async coroutine as a Deferred (work as JDK Future or JS Promise)
            // 2) Return elements consumed from async buffer
            val deferred = kotlinx.coroutines.experimental.async(context) {
                try {
                    while (true) {
                        sink.send(channel.receive())
                    }
                } catch (e: Throwable) {
                    if (e is ClosedReceiveChannelException) sink.close(null)
                    else sink.close(e)
                }
                collector?.invoke()
            }

            // 1) Get input elements and put them in async buffer
            var cause: Throwable? = null
            try {
                this@async.consume<Unit>(object : Sink<E> {
                    override suspend fun send(item: E) {
                        channel.send(Element(item))
                    }

                    override fun close(cause: Throwable?) {
                        cause?.let { throw it }
                    }
                })
            } catch (e: Throwable) {
                cause = e
            }
            val closeCause = cause ?: ClosedReceiveChannelException("close")
            channel.send(Element(closeCause = closeCause))

            return deferred.await() // suspend and return the value of the Deferred
        }
    }
}
