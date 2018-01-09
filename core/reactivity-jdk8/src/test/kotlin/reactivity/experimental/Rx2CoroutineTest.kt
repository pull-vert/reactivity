package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.rx2.rxFlowable
import org.junit.Ignore
import org.junit.Test

class Rx2CoroutineTest: TestBase() {

    @Ignore
    @Test
    fun `observable builder with backpressure`() = runTest {
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