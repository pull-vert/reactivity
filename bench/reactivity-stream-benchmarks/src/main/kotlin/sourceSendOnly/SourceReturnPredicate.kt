package sourceSendOnly

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import reactivity.experimental.channel.SpScChannel
import reactivity.experimental.channel.spsc2.SpScChannel2
import reactivity.experimental.channel.spsc3.SpScChannel3
import reactivity.experimental.channel.spsc4.SpScChannel4
import kotlin.coroutines.experimental.CoroutineContext

// -------------- Model definitions

interface SourceReturnPredicate<out E> {
    suspend fun consume(sink: Sink<E>, returnPredicate: (() -> Any?)? = null): Any?
    companion object Factory
}

interface Sink<in E> {
    suspend fun send(item: E)
    fun close(cause: Throwable?)
}

// -------------- Factory (initial/producing) operations

fun SourceReturnPredicate.Factory.range(start: Int, count: Int): SourceReturnPredicate<Int> = object : SourceReturnPredicate<Int> {
    suspend override fun consume(sink: Sink<Int>, returnPredicate: (() -> Any?)?): Any? {
        var cause: Throwable? = null
        try {
            for (i in start until (start + count)) {
                sink.send(i)
            }
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
        return returnPredicate?.invoke()
    }
}

// -------------- Terminal (final/consuming) operations

inline suspend fun <E, R> SourceReturnPredicate<E>.fold2(initial: R, crossinline operation: (acc: R, E) -> R): R {
    var acc = initial
    return consume(object : Sink<E> {
        suspend override fun send(item: E) {
            acc = operation(acc, item)
        }

        override fun close(cause: Throwable?) {
            cause?.let { throw it }
        }
    }, returnPredicate = { return@consume acc}) as R
}

// -------------- Intermediate (transforming) operations

inline fun <E> SourceReturnPredicate<E>.filter2(crossinline predicate: (E) -> Boolean) = object : SourceReturnPredicate<E> {
    suspend override fun consume(sink: Sink<E>, returnPredicate: (() -> Any?)?): Any? {
        var cause: Throwable? = null
        try {
            this@filter2.consume(object : Sink<E> {
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
        return returnPredicate?.invoke()
    }
}

fun <E : Any> SourceReturnPredicate<E>.async(context: CoroutineContext, buffer: Int = 0): SourceReturnPredicate<E> {
    val channel = SpScChannel<E>(buffer)
    return object : SourceReturnPredicate<E> {
        suspend override fun consume(sink: Sink<E>, returnPredicate: (() -> Any?)?): Any? {
            val deferred = async(context) {
                try {
                    while (true) {
                        sink.send(channel.receive())
                    }
                } catch (e: Throwable) {
                    if (e is ClosedReceiveChannelException) sink.close(null)
                    else sink.close(e)
                }
                returnPredicate?.invoke()
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

            return deferred.await()
        }
    }
}

fun <E : Any> SourceReturnPredicate<E>.async2(context: CoroutineContext, buffer: Int = 0): SourceReturnPredicate<E> {
    val channel = SpScChannel2<E>(buffer)
    return object : SourceReturnPredicate<E> {
        suspend override fun consume(sink: Sink<E>, returnPredicate: (() -> Any?)?): Any? {
            // Get return value of async coroutine as a Deferred (work as JDK Future or JS Promise)
            val deferred = async(context) {
                try {
                    while (true) {
                        sink.send(channel.receive())
                    }
                } catch (e: Throwable) {
                    if (e is ClosedReceiveChannelException) sink.close(null)
                    else sink.close(e)
                }
                returnPredicate?.invoke()
            }

            var cause: Throwable? = null
            try {
                this@async2.consume(object : Sink<E> {
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

            return deferred.await() // suspend and return the value of the Deferred
        }
    }
}

fun <E : Any> SourceReturnPredicate<E>.async3(context: CoroutineContext, buffer: Int = 0): SourceReturnPredicate<E> {
    val channel = SpScChannel3<E>(buffer)
    return object : SourceReturnPredicate<E> {
        suspend override fun consume(sink: Sink<E>, returnPredicate: (() -> Any?)?): Any? {
            // Get return value of async coroutine as a Deferred (work as JDK Future or JS Promise)
            val deferred = async(context) {
                try {
                    while (true) {
                        sink.send(channel.receive())
                    }
                } catch (e: Throwable) {
                    if (e is ClosedReceiveChannelException) sink.close(null)
                    else sink.close(e)
                }
                returnPredicate?.invoke()
            }

            var cause: Throwable? = null
            try {
                this@async3.consume(object : Sink<E> {
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

            return deferred.await() // suspend and return the value of the Deferred
        }
    }
}

fun <E : Any> SourceReturnPredicate<E>.async4(context: CoroutineContext, buffer: Int = 0): SourceReturnPredicate<E> {
    val channel = SpScChannel4<E>(buffer)
    return object : SourceReturnPredicate<E> {
        suspend override fun consume(sink: Sink<E>, returnPredicate: (() -> Any?)?): Any? {
            // Get return value of async coroutine as a Deferred (work as JDK Future or JS Promise)
            val deferred = async(context) {
                try {
                    while (true) {
                        sink.send(channel.receive())
                    }
                } catch (e: Throwable) {
                    if (e is ClosedReceiveChannelException) sink.close(null)
                    else sink.close(e)
                }
                returnPredicate?.invoke()
            }

            var cause: Throwable? = null
            try {
                this@async4.consume(object : Sink<E> {
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

            return deferred.await() // suspend and return the value of the Deferred
        }
    }
}

//interface Sink<in E> {
//    suspend fun send(element: Element<E>)
//}
//
///**
// * Element stored in the buffer
// */
//data class Element<out E>(
//        val item: E? = null,
//        val closed: Closed? = null
//)
//
//data class Closed(
//        val cause: Throwable? = null
//)
