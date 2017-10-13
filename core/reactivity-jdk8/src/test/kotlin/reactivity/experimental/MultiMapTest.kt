package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should be greater than`
import org.amshove.kluent.`should be less than`
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Test
import reactivity.experimental.core.schedulerFixedThreadPoolContext
import reactivity.experimental.core.schedulerFromCoroutineContext

class MultiMapTest {
    @Test
    fun `multi from range map simple`() = runBlocking<Unit> {
        var result = 0
        val source = Multi.fromRange(1, 5) // a fromRange of five numbers
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { println("Finally") }         // ... into what's going on
                .map(schedulerFromCoroutineContext(coroutineContext)) { number ->
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
    fun `multi from iterable map with Exception cancellation`() = runBlocking {
        // create a publisher that produces number 1
        var finally = false
        var onError = false
        var onComplete = false
        val source = (1..5).toMulti()
                .map(schedulerFromCoroutineContext(coroutineContext)) { number ->
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

    @Test
    fun `multi builder map FixedThreadPoolContext`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var source = multi(schedulerFromCoroutineContext(coroutineContext)) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
        // subscribe on another thread with a slow subscriber using Multi
        var start: Long? = null
        var time: Long? = null
        source.map(schedulerFixedThreadPoolContext(3, "test")) { it * 2 }
                .doOnSubscribe {
                    start = System.currentTimeMillis()
                    println("starting timer")
                }
                .doOnComplete {
                    val end = System.currentTimeMillis()
                    time = end - start!!
                    println("Completed in $time ms")
                }.consumeEach { println(it); delay(300) }

        delay(700) // suspend the main thread for a few time
        time!! `should be greater than` 600
        time!! `should be less than` 900
    }
}