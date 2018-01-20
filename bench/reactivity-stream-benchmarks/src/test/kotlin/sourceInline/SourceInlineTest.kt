package sourceInline

import benchmark.N
import benchmark.isGood
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import reactivity.experimental.channel.*
import kotlin.test.assertEquals

class SourceInlineTest {
    @Test
    fun testSourceInlineDeep() = runBlocking {
        val value = SourceInline
                .range(1, N)
                .async(newSingleThreadContext("test"), buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInlineDeep : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }
}