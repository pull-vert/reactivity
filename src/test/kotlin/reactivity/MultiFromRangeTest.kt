package reactivity

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal`
import org.junit.Test

class MultiFromRangeTest {
    @Test
    fun `multi from range test`() = runBlocking {
        // create a publisher that produces numbers from 1 to 3 with 200ms delays between them
        val source = multiFromRange(1, 3)
        // print elements from the source
        var count = 0
        println("Elements:")
        source.consumeEach { // consume elements from it
            count++
            println(it)
        }
        // print elements from the source AGAIN
        println("Again:")
        source.consumeEach { // consume elements from it
            count++
            println(it)
        }
        count `should equal` 6
    }
}