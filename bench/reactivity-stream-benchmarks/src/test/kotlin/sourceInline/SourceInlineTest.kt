package sourceInline

import benchmark.N
import benchmark.isGood
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import sourcesInline.*
import kotlin.test.assertEquals

class SourceInlineTest {

    @Test
    fun testSourceInline() = runBlocking {
        val value = SourceInline
                .range(1, N)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceInlineDeep : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun testSourceInlineDeepQuick() = runBlocking {
        val value = SourceInline
                .range(1, 10)
                .async(buffer = 8)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceInlineDeepQuick : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    @Test
    fun testSourceInlineMediumBuffer() = runBlocking {
        val value = SourceInline
                .range(1, 1_000)
                .async(buffer = 4)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceCollectorMediumBuffer : value = $value run on ${Thread.currentThread().name}")
        assertEquals(125500, value) // 1_000
    }

    @Test
    fun testSourceInlineDeep() = runBlocking {
        val value = SourceInline
                .range(1, N)
                .async(buffer = 128)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceInlineDeep : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }
}