package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.rx2.rxFlowable
import org.amshove.kluent.`should be greater than`
import org.amshove.kluent.`should be less than`
import org.junit.Ignore
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class MultiCoroutineBuilderTest {

    @Test
    fun `multi builder with backpressure test`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        val source = multi(coroutineContext) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
        // subscribe on another thread with a slow subscriber using Multi
        var start: Instant? = null
        var time: Long? = null
        source
                .publishOn(Schedulers.emptyCoroutineContext(), false, 1) // specify buffer size of 1 item
                .doOnSubscribe {
                    start = Instant.now()
                }
                .doOnComplete {
                    val end = Instant.now()
                    time = ChronoUnit.MILLIS.between(start, end)
                    println("Completed in $time ms" )
                }
                .subscribe { x ->
                    Thread.sleep(500) // 500ms to process each item
                    println("Processed $x")
                }
        delay(2000) // suspend the main thread for a few seconds
        time!! `should be greater than` 1000
        time!! `should be less than` 1500
    }

    @Ignore
    @Test
    fun `observable builder with backpressure test`() = runBlocking {
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
        delay(2000) // suspend the main thread for a few seconds
    }
}