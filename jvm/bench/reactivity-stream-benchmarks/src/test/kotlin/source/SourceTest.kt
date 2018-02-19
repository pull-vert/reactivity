package source

import benchmark.N
import benchmark.isGood
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test

class SourceTest {
    @Test
    fun testSource() = runBlocking {
        println(Source
                .range(1, N)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b }))
    }
}