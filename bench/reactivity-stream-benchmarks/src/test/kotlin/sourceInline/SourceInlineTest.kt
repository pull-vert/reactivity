package sourceInline

import benchmark.isGood
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class SourceInlineTest {
    @Test
    fun testSourceInlineDeep() = runBlocking {
        println("testSourceInlineDeep start")
        val value = SourceInline
                .range(1, 10)
                .async(newSingleThreadContext("test"), buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInlineDeep retour = $value")
        assertEquals(12, value)
        delay(5000)
    }
}