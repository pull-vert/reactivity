package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.rx2.rxFlowable
import org.amshove.kluent.`should equal`
import org.junit.Ignore
import org.junit.Test

class MultiCoroutineBuilderTest {

    @Test
    fun `multi builder 2 consumers 1 slow consumer`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var start: Long? = null
        var time: Long?
        val source = multi(Schedulers.fromCoroutineContext(coroutineContext)) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
                .doOnSubscribe {
                    start = System.currentTimeMillis()
                }
                .doOnComplete {
                    val end = System.currentTimeMillis()
                    time = end - start!!
                    println("Completed in $time ms")
                }

        var count = 0
        source.subscribe{x ->
            println("first consumer: Processed $x")
            count++
        }
        // print elements from the source AGAIN
        source.subscribe{x ->
            Thread.sleep(500) // 500ms to process each item
            println("second consumer: Processed $x")
            count++
        }
        delay(2000) // suspend the main thread for a few seconds
        count `should equal` 6
    }

    @Ignore
    @Test
    fun `observable builder with backpressure`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread
        val source = rxFlowable(coroutineContext) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
        // subscribe on another thread with a slow subscriber using Rx
        source
                .observeOn(io.reactivex.schedulers.Schedulers.io(), false, 1) // specify buffer size of 1 item
                .doOnComplete { println("Complete") }
                .subscribe { x ->
                    Thread.sleep(500) // 500ms to process each item
                    println("Processed $x")
                }

//        Flux.fromArray(intArrayOf(1, 2, 3).toTypedArray())
//                .groupBy
        delay(2000) // suspend the main thread for a few seconds
    }
}