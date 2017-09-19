package reactivity.core.experimental

import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal`
import org.junit.Test

class SoloFromValueTest {
    @Test
    fun `solo from value 2 consumers`() = runBlocking {
        // create a publisher that produces number 1
        val source = SoloBuilder.fromValue(1)
        // print element from the source
        var count = 0
        println("first consumer:")
        source.consumeUnique {
            // consume elements from it
            count++
            println(it)
        }
        // print element from the source AGAIN
        println("second consumer:")
        source.consumeUnique {
            // consume elements from it
            count++
            println(it)
        }
        count `should equal` 2
    }
}