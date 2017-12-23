package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SoloSubscribeTest {

    @Test
    fun `solo from value with Exception cancellation`() = runBlocking {
        var finally = false
        var onError = false
        var onComplete = false
        val han = 1.toSolo() // a fromValue of a number
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // print element from the source
        println("empty consumer:")
        // iterate over the source fully : no backpressure = request(Long.maxValue)
        han.subscribe(onNext =  {
            throw Exception("vilain exception !!")
        }, onError = { t ->
            assertEquals("vilain exception !!", t.message!!)
            onError = true
        } , onComplete = {
            onComplete = true
        })
        delay(100)
        assertTrue(onError)
        assertFalse(onComplete)
        assertTrue(finally)
    }

    @Test
    fun `solo from value subscription with subscribe onNext function`() = runBlocking {
        var finally = false
        var onNext = false
        val han = 1.toSolo() // a fromValue of a number
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doOnNext {
                    println(it)
                    onNext = true
                }
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully : no backpressure = request(Long.maxValue)
        han.subscribe()
        delay(100)
        assertTrue(onNext)
        assertTrue(finally)
    }
}