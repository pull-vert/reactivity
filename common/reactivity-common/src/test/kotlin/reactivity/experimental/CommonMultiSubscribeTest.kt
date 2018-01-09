package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonMultiSubscribeTest: TestBase() {
    @Suppress("NAMED_ARGUMENTS_NOT_ALLOWED")
    @Test
    fun `multi from range with Exception cancellation`() = runTest {
        // create a publisher that produces number 1
        var finally = false
        var onError = false
        var onComplete = false
        val source = (1..5).toMulti()
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // print element from the source
        println("empty consumer:")
        source.subscribe(onNext =  { v ->
            if (3 == v) throw Exception("vilain exception !!")
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
    fun `multi from range subscription with subscribe onNext function`() = runTest {
        var finally = false
        var onNext = false
        val source =(1..5).toMulti() // a fromRange of five numbers
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") }         // ... into what's going on
                .doOnNext {
                    println(it)
                    onNext = true
                }
        // iterate over the source fully : no backpressure = request(Long.maxValue)
        source.subscribe()
        delay(100)
        assertTrue(finally)
        assertTrue(onNext)
    }
}