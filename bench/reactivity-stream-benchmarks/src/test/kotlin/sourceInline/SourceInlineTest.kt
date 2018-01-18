package sourceInline

import benchmark.isGood
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class SourceInlineTest {
    @Test
    fun testSourceInlineDeep() = runBlocking {
        val value = SourceInline
                .range(1, 10)
                .async(coroutineContext, buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println(value)
        assertEquals(12, value)
    }
}