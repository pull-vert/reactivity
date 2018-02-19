package reactivity.experimental.jdk8

import reactivity.experimental.Multi
import reactivity.experimental.Sink
import reactivity.experimental.toMulti
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

// -------------- Top level extensions

fun <T> Stream<T>.toMulti() = object : Multi<T> {
    suspend override fun consume(sink: Sink<T>) {
        var cause: Throwable? = null
        try {
            for (x in this@toMulti) { sink.send(x) }
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
    }
}
fun IntStream.toMulti() = this.toArray().toMulti()
fun LongStream.toMulti() = this.toArray().toMulti()
fun DoubleStream.toMulti() = this.toArray().toMulti()