package reactivity.experimental.jdk8

import reactivity.experimental.toMulti
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

// -------------- Top level extensions

fun <T> Stream<T>.toMulti() = this.toArray().toMulti()
fun IntStream.toMulti() = this.toArray().toMulti()
fun LongStream.toMulti() = this.toArray().toMulti()
fun DoubleStream.toMulti() = this.toArray().toMulti()