package sourceInline

import benchmark.N
import benchmark.isGood
import channel.*
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class SourceInlineTest {
    @Test
    @Ignore
    fun testSourceInlineDeep() = runBlocking {
        val value = SourceInline
                .range(1, N)
                .async(newSingleThreadContext("test"), buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInlineDeep : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    @Ignore
    fun testSourceInlineDeepQuick() = runBlocking {
        val value = SourceInline
                .range(1, 10)
                .async(newSingleThreadContext("test"), buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInlineDeepQuick : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }
}