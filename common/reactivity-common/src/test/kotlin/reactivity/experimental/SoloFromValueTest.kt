package reactivity.experimental

import kotlin.test.Test
import kotlin.test.assertEquals

class SoloFromValueTest: TestBase() {
    @Test
    fun `solo from value 2 consumers`() = runTest {
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
        assertEquals(2, count)
    }

    @Test
    fun `solo from value with cancellation`() = runTest {
        // create a publisher that produces number 1
        val han = 1.toSolo()
        // print element from the source
        println("empty consumer:")
        han.openSubscription().use {
            // do nothing, will close without consuming value
        }
    }
}