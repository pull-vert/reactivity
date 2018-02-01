package sourceInline

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import sourceInline.channel.SpScChannelClose
import kotlin.coroutines.experimental.CoroutineContext

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

fun <E> SourceInline<E>.delay(time: Int) = object : SourceInline<E> {
    override fun consume(sink: Sink<E>) {
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

fun <E : Any> SourceInline<E>.async(context: CoroutineContext = DefaultDispatcher, buffer: Int = 0): SourceInline<E> {
    val channel = SpScChannelClose<E>(buffer)
    return object : SourceInline<E> {
        override fun consume(sink: Sink<E>) {
            launch(context) {
                try {
                    while (true) {
                        sink.send(channel.receive())
                    }
                } catch (e: Throwable) {
                    if (e is ClosedReceiveChannelException) sink.close(null)
                    else sink.close(e)
                }
            }

            var cause: Throwable? = null
            try {
                this@async.consume(object : Sink<E> {
                    suspend override fun send(item: E) {
                        channel.send(item)
                    }

                    override fun close(cause: Throwable?) {
                        cause?.let { throw it }
                    }
                })
            } catch (e: Throwable) {
                cause = e
            }
            channel.close(cause)
        }
    }
}