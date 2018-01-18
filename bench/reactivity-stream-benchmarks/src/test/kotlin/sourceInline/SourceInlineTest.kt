package sourceInline

import benchmark.N
import benchmark.isGood
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import sourceInline.filter2
import sourceInline.fold2
import sourceInline.range

class SourceInlineTest {
    @Test
    fun testSourceInlineDeep() = runBlocking {
        println(SourceInline
                .range(1, N)
                .async(coroutineContext, buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b }))
    }
}