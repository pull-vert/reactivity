package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertTrue

class MultiFromArrayTest {
    @Test
    fun `multi from Array with MultiBuilder`() = runBlocking {
        var finally = false
        val source = MultiBuilder.fromArray(arrayOf("0", "58")) // an array of Strings
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        delay(100)
        assertTrue(finally)
    }
}