package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal to`
import org.junit.Test

class MultiFromIterableTest {
    @Test
    fun `multi from Iterable inline subscription`() = runBlocking<Unit> {
        var finally = false
        val source = listOf("0", "58").toMulti() // a list of Strings
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        delay(100)
        finally `should equal to` true
    }

    @Test
    fun `multi from Iterable static subscription`() = runBlocking<Unit> {
        var finally = false
        val source = MultiBuilder.fromIterable(listOf("0", "58")) // a list of Strings
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        delay(100)
        finally `should equal to` true
    }
}