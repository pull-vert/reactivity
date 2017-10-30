package reactivity.experimental

import kotlinx.coroutines.experimental.reactive.openSubscription
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal`
import org.junit.Test

class SoloFromValueTest {
    @Test
    fun `solo from value 2 consumers`() = runBlocking {
        // create a publisher that produces number 1
        val han = 1.toSolo()
        // print element from the source
        var count = 0
        println("first consumer:")
        han.consumeUnique {
            // consume elements from it
            count++
            println(it)
        }
        // print element from the source AGAIN
        println("second consumer:")
        han.consumeUnique {
            // consume elements from it
            count++
            println(it)
        }
        count `should equal` 2
    }

    @Test
    fun `solo from value with cancellation`() = runBlocking {
        // create a publisher that produces number 1
        val han = 1.toSolo()
        // print element from the source
        println("empty consumer:")
        han.openSubscription().use {
            // do nothing, will close without consuming value
        }
    }
}