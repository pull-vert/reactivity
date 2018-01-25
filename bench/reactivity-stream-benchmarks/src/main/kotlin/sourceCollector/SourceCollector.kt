package sourceCollector

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import temp.SpScChannel6
import temp.spsc7.SpScChannel7
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
    suspend override fun <T> consume(sink: Sink<Int>, collector: (() -> T)?): T? {
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
        suspend override fun send(item: E) {
            acc = operation(acc, item)
        }

        override fun close(cause: Throwable?) {
            cause?.let { throw it }
        }
    }, collector = { return@consume acc})!!
}

// -------------- Intermediate (transforming) operations

inline fun <E> SourceCollector<E>.filter(crossinline predicate: (E) -> Boolean) = object : SourceCollector<E> {
    suspend override fun <T> consume(sink: Sink<E>, collector: (() -> T)?): T? {
        var cause: Throwable? = null
        try {
            this@filter.consume<Unit>(object : Sink<E> {
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
        return collector?.invoke()
    }
}

private const val DEFAULT_CLOSE_MESSAGE = "SpScChannel was closed"

/**
 * Element stored in the buffer
 */
data class Element<E : Any>(
        val item: E? = null,
        val closeCause: Throwable? = null
)

fun <E : Any> SourceCollector<E>.async7(context: CoroutineContext, buffer: Int = 0): SourceCollector<E> {
    val channel = SpScChannel7<E>(buffer)
    return object : SourceCollector<E> {
        suspend override fun <T> consume(sink: Sink<E>, collector: (() -> T)?): T? {
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
                collector?.invoke()
            }

            var cause: Throwable? = null
            try {
                this@async7.consume<Unit>(object : Sink<E> {
                    suspend override fun send(item: E) {
                        channel.send(Element(item))
                    }

                    override fun close(cause: Throwable?) {
                        cause?.let { throw it }
                    }
                })
            } catch (e: Throwable) {
                cause = e
            }
            val closeCause = cause ?: ClosedReceiveChannelException(DEFAULT_CLOSE_MESSAGE)
            println("Close : $closeCause")
            channel.send(Element(closeCause = closeCause))

            return deferred.await() // suspend and return the value of the Deferred
        }
    }
}

fun <E : Any> SourceCollector<E>.async6(context: CoroutineContext, buffer: Int = 0): SourceCollector<E> {
    val channel = SpScChannel6<E>(buffer)
    return object : SourceCollector<E> {
        suspend override fun <T> consume(sink: Sink<E>, collector: (() -> T)?): T? {
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
                collector?.invoke()
            }

            var cause: Throwable? = null
            try {
                this@async6.consume<Unit>(object : Sink<E> {
                    suspend override fun send(item: E) {
                        channel.send(Element(item))
                    }

                    override fun close(cause: Throwable?) {
                        cause?.let { throw it }
                    }
                })
            } catch (e: Throwable) {
                cause = e
            }
            val closeCause = cause ?: ClosedReceiveChannelException(DEFAULT_CLOSE_MESSAGE)
            println("Close : $closeCause")
            channel.send(Element(closeCause = closeCause))

            return deferred.await() // suspend and return the value of the Deferred
        }
    }
}

//fun <E : Any> SourceCollector<E>.async5(context: CoroutineContext, buffer: Int = 0): SourceCollector<E> {
//    val channel = SpScChannel5<E>(buffer)
//    return object : SourceCollector<E> {
//        suspend override fun <T> consume(sink: Sink<E>, collector: (() -> T)?): T? {
//            // Get return value of async coroutine as a Deferred (work as JDK Future or JS Promise)
//            val deferred = async(context) {
//                try {
//                    while (true) {
//                        sink.send(channel.receive())
//                    }
//                } catch (e: Throwable) {
//                    if (e is ClosedReceiveChannelException) sink.close(null)
//                    else sink.close(e)
//                }
//                collector?.invoke()
//            }
//
//            var cause: Throwable? = null
//            try {
//                this@async5.consume<Unit>(object : Sink<E> {
//                    suspend override fun send(item: E) {
//                        channel.send(item)
//                    }
//
//                    override fun close(cause: Throwable?) {
//                        cause?.let { throw it }
//                    }
//                })
//            } catch (e: Throwable) {
//                cause = e
//            }
//            channel.close(cause)
//
//            return deferred.await() // suspend and return the value of the Deferred
//        }
//    }
//}
//
//fun <E : Any> SourceCollector<E>.async4(context: CoroutineContext, buffer: Int = 0): SourceCollector<E> {
//    val channel = SpScChannel4<E>(buffer)
//    return object : SourceCollector<E> {
//        suspend override fun <T> consume(sink: Sink<E>, collector: (() -> T)?): T? {
//            // Get return value of async coroutine as a Deferred (work as JDK Future or JS Promise)
//            val deferred = async(context) {
//                try {
//                    while (true) {
//                        sink.send(channel.receive())
//                    }
//                } catch (e: Throwable) {
//                    if (e is ClosedReceiveChannelException) sink.close(null)
//                    else sink.close(e)
//                }
//                collector?.invoke()
//            }
//
//            var cause: Throwable? = null
//            try {
//                this@async4.consume<Unit>(object : Sink<E> {
//                    suspend override fun send(item: E) {
//                        channel.send(item)
//                    }
//
//                    override fun close(cause: Throwable?) {
//                        cause?.let { throw it }
//                    }
//                })
//            } catch (e: Throwable) {
//                cause = e
//            }
//            channel.close(cause)
//
//            return deferred.await() // suspend and return the value of the Deferred
//        }
//    }
//}
//
//fun <E : Any> SourceCollector<E>.async3(context: CoroutineContext, buffer: Int = 0): SourceCollector<E> {
//    val channel = SpScChannel3<E>(buffer)
//    return object : SourceCollector<E> {
//        suspend override fun <T> consume(sink: Sink<E>, collector: (() -> T)?): T? {
//            // Get return value of async coroutine as a Deferred (work as JDK Future or JS Promise)
//            val deferred = async(context) {
//                try {
//                    while (true) {
//                        sink.send(channel.receive())
//                    }
//                } catch (e: Throwable) {
//                    if (e is ClosedReceiveChannelException) sink.close(null)
//                    else sink.close(e)
//                }
//                collector?.invoke()
//            }
//
//            var cause: Throwable? = null
//            try {
//                this@async3.consume<Unit>(object : Sink<E> {
//                    suspend override fun send(item: E) {
//                        channel.send(item)
//                    }
//
//                    override fun close(cause: Throwable?) {
//                        cause?.let { throw it }
//                    }
//                })
//            } catch (e: Throwable) {
//                cause = e
//            }
//            channel.close(cause)
//
//            return deferred.await() // suspend and return the value of the Deferred
//        }
//    }
//}
//
//fun <E : Any> SourceCollector<E>.async2(context: CoroutineContext, buffer: Int = 0): SourceCollector<E> {
//    val channel = SpScChannel2<E>(buffer)
//    return object : SourceCollector<E> {
//        suspend override fun <T> consume(sink: Sink<E>, collector: (() -> T)?): T? {
//            // Get return value of async coroutine as a Deferred (work as JDK Future or JS Promise)
//            val deferred = async(context) {
//                try {
//                    while (true) {
//                        sink.send(channel.receive())
//                    }
//                } catch (e: Throwable) {
//                    if (e is ClosedReceiveChannelException) sink.close(null)
//                    else sink.close(e)
//                }
//                collector?.invoke()
//            }
//
//            var cause: Throwable? = null
//            try {
//                this@async2.consume<Unit>(object : Sink<E> {
//                    suspend override fun send(item: E) {
//                        channel.send(item)
//                    }
//
//                    override fun close(cause: Throwable?) {
//                        cause?.let { throw it }
//                    }
//                })
//            } catch (e: Throwable) {
//                cause = e
//            }
//            channel.close(cause)
//
//            return deferred.await() // suspend and return the value of the Deferred
//        }
//    }
//}
//
//fun <E : Any> SourceCollector<E>.async(context: CoroutineContext, buffer: Int = 0): SourceCollector<E> {
//    val channel = SpScChannel<E>(buffer)
//    return object : SourceCollector<E> {
//        suspend override fun <T> consume(sink: Sink<E>, collector: (() -> T)?): T? {
//            val deferred = async(context) {
//                try {
//                    while (true) {
//                        sink.send(channel.receive())
//                    }
//                } catch (e: Throwable) {
//                    if (e is ClosedReceiveChannelException) sink.close(null)
//                    else sink.close(e)
//                }
//                collector?.invoke()
//            }
//
//            var cause: Throwable? = null
//            try {
//                this@async.consume<Unit>(object : Sink<E> {
//                    suspend override fun send(item: E) {
//                        channel.send(item)
//                    }
//
//                    override fun close(cause: Throwable?) {
//                        cause?.let { throw it }
//                    }
//                })
//            } catch (e: Throwable) {
//                cause = e
//            }
//            channel.close(cause)
//
//            return deferred.await()
//        }
//    }
//}

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
