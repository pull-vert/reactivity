package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonMultiMapTest: TestBase() {
    @Test
    fun `multi from range map simple`() = runTest {
        var result = 0
        val source = (1..5).toMulti(coroutineContext.toScheduler()) // a fromRange of five numbers
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { println("Finally") }         // ... into what's going on
                .map{ it + 1 }
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
        assertEquals(20, result)
    }

    @Suppress("NAMED_ARGUMENTS_NOT_ALLOWED")
    @Test
    fun `multi from iterable map with Exception cancellation`() = runTest {
        // create a publisher that produces number 1
        var finally = false
        var onError = false
        var onComplete = false
        val source = (1..5).toMulti(coroutineContext.toScheduler())
                .map { number ->
                    if (3 == number) throw Exception("vilain exception !!")
                    number
                }
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // print element from the source
        println("empty consumer:")
        source.subscribe(onNext = {
            println(it)
        }, onError = { t ->
            assertEquals("vilain exception !!", t.message!!)
            onError = true
        } , onComplete = {
            onComplete = true
        })
        delay(100)
        assertTrue(finally)
        assertTrue(onError)
        assertFalse(onComplete)
    }

    @Test
    fun `multi builder map`() = runTest {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        val source = multi(coroutineContext.toScheduler()) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
        // subscribe on another thread with a slow subscriber using Multi
        var start: Long? = null
        var time: Long? = null
        source.map { it * 2 }
                .doOnSubscribe {
                    start = currentTimeMillis()
                    println("starting timer")
                }
                .doOnComplete {
                    val end = currentTimeMillis()
                    time = end - start!!
                    println("Completed in $time ms")
                }.consumeEach { println(it); delay(50) }

        delay(200) // suspend the main thread for a few time
        assertTrue(time!! > 100)
        assertTrue(time!! < 150)
    }
}