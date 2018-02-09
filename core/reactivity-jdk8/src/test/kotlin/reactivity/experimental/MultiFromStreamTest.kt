package reactivity.experimental

import org.junit.Test
import reactivity.experimental.jdk8.toMulti
import java.util.stream.IntStream
import kotlin.test.assertEquals

class MultiFromStreamTest: TestBase() {
    @Test
    fun `multi from Stream`() = runTest {
        val value = IntStream.of(1, 2 , 3, 4, 5, 6, 7, 8, 9, 10).toMulti() // a list of Ints
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("multi from Stream : value = $value")
        assertEquals(12, value)
    }
}