package sourceInline

import benchmark.N
import benchmark.isGood
import org.junit.Test
import kotlin.test.assertEquals

class SourceInlineTest {

    @Test
    fun testSourceInline() {
        val value = SourceInline
                .range(1, N)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceInline : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun testSourceInlineDelay() {
        val value = SourceInline
                .range(1, 10)
                .filter { it.isGood() }
                .delay(100)
                .fold(0, { a, b -> a + b })
        println("testSourceInlineDelay : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }
}