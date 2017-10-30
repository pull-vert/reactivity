package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal to`
import org.junit.Test

class MultiFromArrayTest {
    @Test
    fun `multi from Array inline subscription`() = runBlocking<Unit> {
        var finally = false
        val source = arrayOf("0", "58").toMulti() // an array of Strings
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        delay(100)
        finally `should equal to` true
    }

    @Test
    fun `multi from IntArray inline subscription`() = runBlocking<Unit> {
        var finally = false
        val source = intArrayOf(1, 89, 4567).toMulti() // an array of Int
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        delay(100)
        finally `should equal to` true
    }

    @Test
    fun `multi from CharArray inline subscription`() = runBlocking<Unit> {
        var finally = false
        val source = charArrayOf('1','8', 'z').toMulti() // an array of Char
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        delay(100)
        finally `should equal to` true
    }

    @Test
    fun `multi from Array static subscription`() = runBlocking<Unit> {
        var finally = false
        val source = MultiBuilder.fromArray(arrayOf("0", "58")) // an array of Strings
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        delay(100)
        finally `should equal to` true
    }
}