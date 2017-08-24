package reactivity

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.reactive.openSubscription
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal`
import org.junit.Test

class MultiFromRangeTest {
    @Test
    fun `multi from range test`() = runBlocking {
        // create a publisher that produces numbers from 1 to 3
        val source = multiFromRange(1, 3)
        // print elements from the source
        var count = 0
        println("Elements:")
        source.consumeEach {
            // consume elements from it
            count++
            println(it)
        }
        // print elements from the source AGAIN
        println("Again:")
        source.consumeEach {
            // consume elements from it
            count++
            println(it)
        }
        count `should equal` 6
    }

    @Test
    fun `multi subscription and cancellation`() = runBlocking {
        // create a publisher that produces numbers from 1 to 5
        val source = multiFromRange(1, 5)
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { println("Finally") }         // ... into what's going on
        // print elements from the source
        var countElements = 0;
        var cnt = 0
        println("Elements:")
        source.openSubscription().use { channel -> // open channel to the source
            for (x in channel) { // iterate over the channel to receive elements from it
                countElements++
                println(x)
                if (++cnt >= 3) break // break when 3 elements are printed
            }
            // `use` will close the channel when this block of code is complete
        }
        // print elements from the source AGAIN
        cnt = 0
        println("Again:")
        source.openSubscription().use { channel -> // open channel to the source
            for (x in channel) { // iterate over the channel to receive elements from it
                countElements++
                println(x)
                if (++cnt >= 3) break // break when 3 elements are printed
            }
            // `use` will close the channel when this block of code is complete
        }
        countElements `should equal` 6
    }
}