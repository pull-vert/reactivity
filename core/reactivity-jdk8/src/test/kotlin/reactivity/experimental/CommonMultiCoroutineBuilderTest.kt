package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.rx2.rxFlowable
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class CommonMultiCoroutineBuilderTest {

    @Test
    fun `multi builder 2 consumers 1 slow consumer`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var start: Long? = null
        var time: Long?
        val source = multi(schedulerFromCoroutineContext(coroutineContext)) {
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
            Thread.sleep(50) // 50ms to process each item
            println("second consumer: Processed $x")
            count++
        }
        delay(200) // suspend the main thread for a few seconds
        assertEquals(6, count)
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