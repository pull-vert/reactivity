package reactivity.core.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal to`
import org.junit.Test

class SoloSubscribeTest {

    @Test
    fun `solo from value with Exception cancellation`() = runBlocking {
        var finally = false
        var onError = false
        var onComplete = false
        val source = SoloBuilder.fromValue(1) // a fromValue of a number
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // print element from the source
        println("empty consumer:")
        // iterate over the source fully : no backpressure = request(Long.maxValue)
        source.subscribe(onNext =  {
            throw Exception("vilain exception !!")
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
    fun `solo from value subscription with subscribe onNext function`() = runBlocking<Unit> {
        var finally = false
        var onNext = false
        val source = SoloBuilder.fromValue(1) // a fromValue of a number
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doOnNext {
                    println(it)
                    onNext = true
                }
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully : no backpressure = request(Long.maxValue)
        source.subscribe()
        finally `should equal to` true
        onNext `should equal to` true
    }
}