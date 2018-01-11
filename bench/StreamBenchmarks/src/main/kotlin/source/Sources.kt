package source

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

// -------------- Interface definitions

interface Source<out E> {
    suspend fun consume(sink: Sink<E>)
    companion object Factory
}

interface Sink<in E> {
    suspend fun send(item: E)
    fun close(cause: Throwable?)
}

// -------------- Factory (initial/producing) operations

fun <E> source(action: suspend Sink<E>.() -> Unit): Source<E> = object : Source<E> {
    suspend override fun consume(sink: Sink<E>) {
        var cause: Throwable? = null
        try {
            action(sink)
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
    }
}

fun Source.Factory.range(start: Int, count: Int): Source<Int> = source<Int> {
    for (i in start until (start + count)) {
        send(i)
    }
}

// -------------- Terminal (final/consuming) operations

suspend fun <E> Source<E>.consumeEach(action: suspend (E) -> Unit) {
    consume(object : Sink<E> {
        suspend override fun send(item: E) = action(item)
        override fun close(cause: Throwable?) { cause?.let { throw it } }
    })
}

suspend fun <E, R> Source<E>.fold(initial: R, operation: suspend (acc: R, E) -> R): R {
    var acc = initial
    consumeEach {
        acc = operation(acc, it)
    }
    return acc
}

// -------------- Intermediate (transforming) operations

fun <E> Source<E>.filter(predicate: suspend (E) -> Boolean) = source<E> {
    consumeEach {
        if (predicate(it)) send(it)
    }
}

fun <E> Source<E>.async(context: CoroutineContext = DefaultDispatcher, buffer: Int = 0): Source<E> {
    val channel = Channel<E>(buffer)
    return object : Source<E> {
        suspend override fun consume(sink: Sink<E>) {
            launch(context) {
                channel.consumeEach { sink.send(it) }
            }
            this@async.consumeEach { channel.send(it) }
        }
    }
}
