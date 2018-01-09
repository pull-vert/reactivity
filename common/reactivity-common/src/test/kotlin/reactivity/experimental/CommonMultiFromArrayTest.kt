package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlin.test.Test
import kotlin.test.assertTrue

class CommonMultiFromArrayTest : TestBase() {
    @Test
    fun `multi from Array inline subscription`() = runTest {
        var finally = false
        val source = arrayOf("0", "58").toMulti() // an array of Strings
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        delay(100)
        assertTrue(finally)
    }

    @Test
    fun `multi from IntArray inline subscription`() = runTest {
        var finally = false
        val source = intArrayOf(1, 89, 4567).toMulti(coroutineContext.toScheduler()) // an array of Int
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        delay(100)
        assertTrue(finally)
    }

    @Test
    fun `multi from CharArray inline subscription`() = runTest {
        var finally = false
        val source = charArrayOf('1','8', 'z').toMulti(schedulerFromCoroutineContext(coroutineContext)) // an array of Char
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        delay(100)
        assertTrue(finally)
    }
}