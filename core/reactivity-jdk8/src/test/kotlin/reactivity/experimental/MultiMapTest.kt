package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Test
import reactivity.experimental.core.Schedulers

class MultiMapTest {
    @Test
    fun `multi from range map simple`() = runBlocking<Unit> {
        var result = 0
        val source = MultiBuilder.fromRange(1, 5) // a fromRange of five numbers
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { println("Finally") }         // ... into what's going on
                .map(Schedulers.fromCoroutineContext(coroutineContext)) { number ->
                    number + 1
                }
        // iterate over the source fully
        source.consumeEach {
            println(it)
            result = result + it
        }
        /* Notice, how "Finally" is printed before the last element "5".
        It happens because our main function in this example is a coroutine that
        we start with runBlocking coroutine builder. Our main coroutine receives on
        the channel using source.consumeEach { ... } expression.
        The main coroutine is suspended while it waits for the source to emit an item.
        When the last item is emitted by Multi.fromRange(1, 5) it resumes the main coroutine,
        which gets dispatched onto the main thread to print this last element at
        a later point in time, while the source completes and prints "Finally". */
        result `should equal` 20
    }

    @Test
    fun `multi from range map with Exception cancellation`() = runBlocking {
        // create a publisher that produces number 1
        var finally = false
        var onError = false
        var onComplete = false
        val source = MultiBuilder.fromRange(1, 5)
                .map(Schedulers.fromCoroutineContext(coroutineContext)) { number ->
                    if (3 == number) throw Exception("vilain exception !!")
                    number
                }
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // print element from the source
        println("empty consumer:")
        source.subscribe(onNext = {
            println(it)
        }, onError = { t ->
            "vilain exception !!" `should equal to` t.message!!
            onError = true
        } , onComplete = {
            onComplete = true
        })
        delay(100)
        finally `should equal to` true
        onError `should equal to` true
        onComplete `should equal to` false
    }
}