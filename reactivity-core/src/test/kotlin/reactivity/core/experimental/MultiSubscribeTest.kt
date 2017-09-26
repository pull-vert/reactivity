package reactivity.core.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal to`
import org.junit.Test

class MultiSubscribeTest {
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
    fun `multi from range subscription with subscribe onNext function`() = runBlocking<Unit> {
        var finally = false
        var onNext = false
        val source = MultiBuilder.fromRange(1, 5) // a fromRange of five numbers
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") }         // ... into what's going on
                .doOnNext {
                    println(it)
                    onNext = true
                }
        // iterate over the source fully : no backpressure = request(Long.maxValue)
        source.subscribe()
        finally `should equal to` true
        onNext `should equal to` true
    }
}