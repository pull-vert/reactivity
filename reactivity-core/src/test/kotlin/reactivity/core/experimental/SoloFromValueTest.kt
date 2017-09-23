package reactivity.core.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Test

class SoloFromValueTest {
    @Test
    fun `solo from value 2 consumers`() = runBlocking {
        // create a publisher that produces number 1
        val source = SoloBuilder.fromValue(1)
        // print element from the source
        var count = 0
        println("first consumer:")
        source.consumeUnique {
            // consume elements from it
            count++
            println(it)
        }
        // print element from the source AGAIN
        println("second consumer:")
        source.consumeUnique {
            // consume elements from it
            count++
            println(it)
        }
        count `should equal` 2
    }

    @Test
    fun `solo from value with Exception cancellation`() = runBlocking {
        // create a publisher that produces number 1
        var finally = false
        var onError = false
        var onComplete = false
        val source = SoloBuilder.fromValue(1)
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // print element from the source
        println("empty consumer:")
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
    fun `solo from value with cancellation`() = runBlocking {
        // create a publisher that produces number 1
        val source = SoloBuilder.fromValue(1)
        // print element from the source
        println("empty consumer:")
        source.openDeferred().use {
            // do nothing, will close without consuming value
        }
    }

    @Test
    fun `solo from value subscription with subscribe onNext function`() = runBlocking<Unit> {
        var finally = false
        val source = SoloBuilder.fromValue(1) // a fromValue of a number
                .doOnSubscribe { println("OnSubscribe") } // provide some insight
                .doFinally { finally = true; println("Finally") } // ... into what's going on
        // iterate over the source fully : no backpressure = request(Long.maxValue)
        source.subscribe { println(it) }
        finally `should equal to` true
    }
}