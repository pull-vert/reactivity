package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.reactive.openSubscription
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultiFromRangeTest: TestBase() {
    @Test
    fun `multi from range 2 consumers`() = runTest {
        // create a publisher that produces numbers from 1 to 3
        val source = MultiBuilder.fromRange(1, 3)
        // print elements from the source
        var count = 0
        println("first consumer:")
        source.consumeEach {
            // consume elements from it
            count++
            println(it)
        }
        // print elements from the source AGAIN
        println("second consumer:")
        source.consumeEach {
            // consume elements from it
            count++
            println(it)
        }
        assertEquals(6, count)
    }

    @Test
    fun `multi from range subscription with cancellation`() = runTest {
        var finally = false
        // create a publisher that produces numbers from 1 to 5
        val source = MultiBuilder.fromRange(1, 5)
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // print elements from the source
        var cnt = 0
        source.openSubscription().use { channel -> // open channel to the source
            for (x in channel) { // iterate over the channel to receive elements from it
                println(x)
                if (++cnt >= 3) break // break when 3 elements are printed
            }
            // `use` will close the channel when this block of code is complete
        }
        assertEquals(3, cnt)
        assertTrue(finally)
    }

    @Test
    fun `multi from range subscription without cancellation`() = runTest {
        var finally = false
        val source = MultiBuilder.fromRange(1, 5) // a fromRange of five numbers
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        /* Notice, how "Finally" is printed before the last element "5".
        It happens because our main function in this example is a coroutine that
        we start with runTest coroutine builder. Our main coroutine receives on
        the channel using source.consumeEach { ... } expression.
        The main coroutine is suspended while it waits for the source to emit an item.
        When the last item is emitted by Multi.fromRange(1, 5) it resumes the main coroutine,
        which gets dispatched onto the main thread to print this last element at
        a later point in time, while the source completes and prints "Finally". */
        delay(100)
        assertTrue(finally)
    }

    @Test
    fun `multi from range subscription without cancellation, and Common pool scheduler`() = runTest {
        var finally = false
        val source = MultiBuilder.fromRange(SCHEDULER_COMMON_POOL_DISPATCHER,
                1, 5) // a fromRange of five numbers
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        /* Notice, how "Finally" is printed before the last element "5".
        It happens because our main function in this example is a coroutine that
        we start with runTest coroutine builder. Our main coroutine receives on
        the channel using source.consumeEach { ... } expression.
        The main coroutine is suspended while it waits for the source to emit an item.
        When the last item is emitted by Multi.fromRange(1, 5) it resumes the main coroutine,
        which gets dispatched onto the main thread to print this last element at
        a later point in time, while the source completes and prints "Finally". */
        assertTrue(finally)
    }
}