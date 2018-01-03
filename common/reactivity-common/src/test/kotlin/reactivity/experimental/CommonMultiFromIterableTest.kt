package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlin.test.Test
import kotlin.test.assertTrue

class CommonMultiFromIterableTest : TestBase() {
    @Test
    fun `multi from Iterable inline subscription`() = runTest {
        var finally = false
        val source = listOf("0", "58").toMulti() // a list of Strings
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        delay(100)
        assertTrue(finally)
    }
}