package sourceInline

import coroutines.launchSimple
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import kotlinx.coroutines.experimental.launch
import quasar.QuasarFiber
import reactivity.experimental.Element
import reactivity.experimental.channel.SpScChannel
import kotlin.coroutines.experimental.CoroutineContext

// -------------- Interface definitions

interface SourceInline<out E> {
    suspend fun consume(sink: Sink<E>)
    companion object Factory
}

interface Sink<in E> {
    suspend fun send(item: E)
    fun close(cause: Throwable?)
}

// -------------- Factory (initial/producing) operations

fun SourceInline.Factory.range(start: Int, count: Int): SourceInline<Int> = object : SourceInline<Int> {
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

suspend fun <E, R> SourceInline<E>.fold(initial: R, operation: suspend (acc: R, E) -> R): R {
    var acc = initial
    consume(object : Sink<E> {
        override suspend fun send(item: E) {
            println("fold $item on thread=${Thread.currentThread().name}")
            acc = operation(acc, item)
        }
        override fun close(cause: Throwable?) { cause?.let { throw it } }
    })
    return acc
}

suspend inline fun <E, R> SourceInline<E>.fold2(initial: R, crossinline operation: (acc: R, E) -> R): R {
    var acc = initial
    consume(object : Sink<E> {
        override suspend fun send(item: E) {
            acc = operation(acc, item)
        }
        override fun close(cause: Throwable?) { cause?.let { throw it } }
    })
    return acc
}

// -------------- Intermediate (transforming) operations

fun <E> SourceInline<E>.filter(predicate: suspend (E) -> Boolean) = object : SourceInline<E> {
    override suspend fun consume(sink: Sink<E>) {
        var cause: Throwable? = null
        try {
            this@filter.consume(object : Sink<E> {
                override suspend fun send(item: E) {
                    println("filter $item on thread=${Thread.currentThread().name}")
                    if (predicate(item)) sink.send(item)
                }
                override fun close(cause: Throwable?) { cause?.let { throw it } }
            })
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
    }
}

inline fun <E> SourceInline<E>.filter2(crossinline predicate: (E) -> Boolean) = object : SourceInline<E> {
    override suspend fun consume(sink: Sink<E>) {
        var cause: Throwable? = null
        try {
            this@filter2.consume(object : Sink<E> {
                override suspend fun send(item: E) {
                    if (predicate(item)) sink.send(item)
                }
                override fun close(cause: Throwable?) { cause?.let { throw it } }
            })
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
    }
}

fun <E : Any> SourceInline<E>.asyncSpSc(buffer: Int, context: CoroutineContext = DefaultDispatcher): SourceInline<E> {
    val channel = SpScChannel<E>(buffer)
    return object : SourceInline<E> {
        override suspend fun consume(sink: Sink<E>) {
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
                this@asyncSpSc.consume(object : Sink<E> {
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
            val closeCause = cause ?: ClosedReceiveChannelException("Close")
            channel.send(Element(closeCause = closeCause))
        }
    }
}

fun <E : Any> SourceInline<E>.asyncSpScLaunchSimple(buffer: Int, context: CoroutineContext = QuasarFiber): SourceInline<E> {
    val channel = SpScChannel<E>(buffer)
    return object : SourceInline<E> {
        override suspend fun consume(sink: Sink<E>) {
            launchSimple(context) {
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
                this@asyncSpScLaunchSimple.consume(object : Sink<E> {
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
            val closeCause = cause ?: ClosedReceiveChannelException("Close")
            channel.send(Element(closeCause = closeCause))
        }
    }
}

fun <E : Any> SourceInline<E>.asyncMpMc(context: CoroutineContext = DefaultDispatcher, buffer: Int = 0): SourceInline<E> {
    val channel = Channel<E>(buffer)
    return object : SourceInline<E> {
        override suspend fun consume(sink: Sink<E>) {
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
                this@asyncMpMc.consume(object : Sink<E> {
                    override suspend fun send(item: E) {
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



