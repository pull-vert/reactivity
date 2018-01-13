package source

import benchmark.N
import benchmark.isGood
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import sourceInline.filter2
import sourceInline.fold2
import sourceInline.range

class SourceTest {
    @Test
    fun testSourceInlineDeep() = runBlocking {
        println(sourceInline.SourceInline
                .range(1, N)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b }))
    }
}