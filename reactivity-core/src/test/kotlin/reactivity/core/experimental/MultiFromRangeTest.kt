package reactivity.core.experimental

import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.reactive.openSubscription
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Test

class MultiFromRangeTest {
    @Test
    fun `multi from range 2 consumers`() = runBlocking {
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
        count `should equal` 6
    }

    @Test
    fun `multi from range subscription with cancellation`() = runBlocking {
        // create a publisher that produces numbers from 1 to 5
        val source = MultiBuilder.fromRange(1, 5)
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { println("Finally") }         // ... into what's going on
        // print elements from the source
        var cnt = 0
        source.openSubscription().use { channel -> // open channel to the source
            for (x in channel) { // iterate over the channel to receive elements from it
                println(x)
                if (++cnt >= 3) break // break when 3 elements are printed
            }
            // `use` will close the channel when this block of code is complete
        }
        cnt `should equal` 3
    }

    @Test
    fun `multi from range with Exception cancellation`() = runBlocking {
        // create a publisher that produces number 1
        var finally = false
        var onError = false
        var onComplete = false
        val source = MultiBuilder.fromRange(1, 5)
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // print element from the source
        println("empty consumer:")
        source.subscribe(onNext =  { v ->
            if (3 == v) throw Exception("vilain exception !!")
        }, onError = { t ->
            "vilain exception !!" `should equal to` t.message!!
            onError = true
        } , onComplete = {
            onComplete = true
        })
        finally `should equal to` true
        onError `should equal to` true
        onComplete `should equal to` false
    }

    @Test
    fun `multi from range subscription without cancellation`() = runBlocking<Unit> {
        val source = MultiBuilder.fromRange(1, 5) // a fromRange of five numbers
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { println("Finally") }         // ... into what's going on
        // iterate over the source fully
        source.consumeEach { println(it) }
        /* Notice, how "Finally" is printed before the last element "5".
        It happens because our main function in this example is a coroutine that
        we start with runBlocking coroutine builder. Our main coroutine receives on
        the channel using source.consumeEach { ... } expression.
        The main coroutine is suspended while it waits for the source to emit an item.
        When the last item is emitted by Multi.fromRange(1, 5) it resumes the main coroutine,
        which gets dispatched onto the main thread to print this last element at
        a later point in time, while the source completes and prints "Finally". */
    }

    @Test
    fun `multi from range subscription with subscribe onNext function`() = runBlocking<Unit> {
        var finally = false
        val source = MultiBuilder.fromRange(1, 5) // a fromRange of five numbers
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") }         // ... into what's going on
        // iterate over the source fully : no backpressure = request(Long.maxValue)
        source.subscribe { println(it) }
        finally `should equal to` true
    }
}