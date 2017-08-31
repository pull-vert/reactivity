package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.rx2.rxFlowable
import org.amshove.kluent.`should be greater than`
import org.amshove.kluent.`should be less than`
import org.amshove.kluent.`should equal`
import org.junit.Ignore
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class MultiCoroutineBuilderTest {

    @Test
    fun `multi builder with backpressure`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var source = multi(coroutineContext) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
        // subscribe on another thread with a slow subscriber using Multi
        var start: Instant? = null
        var time: Long? = null
        source = source
                .publishOn(Schedulers.emptyCoroutineContext(), false, 3) // specify buffer size of 1 item
                .doOnSubscribe {
                    start = Instant.now()
                    println("starting timer")
                }
                .doOnComplete {
                    val end = Instant.now()
                    time = ChronoUnit.MILLIS.between(start, end)
                    println("Completed in $time ms")
                }

        source.subscribe{x ->
            println("Processed $x")
            Thread.sleep(500) // 500ms to process each item
        }

        delay(2000) // suspend the main thread for a few seconds
        time!! `should be greater than` 1500
        time!! `should be less than` 2000
    }

    @Ignore
    @Test
    fun `multi builder 2 consumers`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var start: Instant? = null
        var time: Long? = null
        val source = multi(coroutineContext) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
                .doOnSubscribe {
                    start = Instant.now()
                }
                .doOnComplete {
                    val end = Instant.now()
                    time = ChronoUnit.MILLIS.between(start, end)
                    println("Completed in $time ms")
                }

        var count = 0
        println("first consumer:")
        source.consumeEach {
            // consume elements from it
            count++
            Thread.sleep(500) // 500ms to process each item
            println(it)
        }
        // print elements from the source AGAIN
        println("second consumer:")
        source.consumeEach {
            // consume elements from it
            count++
            println(it)
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
        delay(2000) // suspend the main thread for a few seconds
    }
}