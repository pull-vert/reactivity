package reactivity.experimental

import org.junit.Test
import reactivity.experimental.jdk8.toMulti
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream
import kotlin.test.assertEquals

fun Long.isGood() = this % 4L == 0L
fun Double.isGood() = this % 4.0 == 0.0

class MultiFromStreamTest: TestBase() {
    @Test
    fun `Multi from Stream`() = runTest {
        val value = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).toMulti() // a list of Ints
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("Multi from Stream : value = $value")
        assertEquals(12, value)
    }

    @Test
    fun `Multi from Int Stream`() = runTest {
        val value = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).toMulti()
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("Multi from Int Stream : value = $value")
        assertEquals(12, value)
    }

    @Test
    fun `Multi from Long Stream`() = runTest {
        val value = LongStream.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L).toMulti()
                .filter { it.isGood() }
                .fold(0L, { a, b -> a + b })
        println("Multi from Long Stream : value = $value")
        assertEquals(12L, value)
    }

    @Test
    fun `Multi from Double Stream`() = runTest {
        val value = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0).toMulti()
                .filter { it.isGood() }
                .fold(0.0, { a, b -> a + b })
        println("Multi from Double Stream : value = $value")
        assertEquals(12.0, value)
    }
}