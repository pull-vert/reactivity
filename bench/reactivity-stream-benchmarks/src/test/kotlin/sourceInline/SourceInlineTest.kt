package sourceInline

import benchmark.N
import benchmark.isGood
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import reactivity.experimental.filter
import reactivity.experimental.fold
import kotlin.test.assertEquals

class SourceInlineTest {

    @Test
    fun testSourceInline() = runBlocking {
        val value = SourceInline
                .range(1, N)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInline : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun testSourceInlineMpMc() = runBlocking {
        val value = SourceInline
                .range(1, N)
                .asyncMpMc(buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInlineDelay : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun testSourceInlineSpSc() = runBlocking {
        val value = SourceInline
                .range(1, N)
                .asyncSpSc(buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInlineDelay : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun testSourceInlineThreadBuffer128SpScLaunchSimpleFjp() = runBlocking {
        val value = SourceInline
                .range(1, 10)
                .asyncSpScLaunchSimpleFjp(buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInlineThreadBuffer128SpScLaunchSimpleFjp : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }
}