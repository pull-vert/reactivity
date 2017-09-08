package reactivity.core.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.rx2.rxFlowable
import org.amshove.kluent.`should be greater than`
import org.amshove.kluent.`should be less than`
import org.amshove.kluent.`should equal`
import org.junit.Ignore
import org.junit.Test
import reactor.core.publisher.Flux

class MultiCoroutineBuilderTest {

    @Test
    fun `multi builder publishOn emptyThreadContext`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var source = multi(coroutineContext) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
        // subscribe on another thread with a slow subscriber using Multi
        var start: Long? = null
        var time: Long? = null
        source = source
                .publishOn(Schedulers.emptyThreadContext(), false, 2) // specify buffer size of 2 items
                .doOnSubscribe {
                    start = System.currentTimeMillis()
                    println("starting timer")
                }
                .doOnComplete {
                    val end = System.currentTimeMillis()
                    time = end - start!!
                    println("Completed in $time ms")
                }

        source.subscribe{x ->
            Thread.sleep(500) // 500ms to process each item
            println("Processed $x")
        }

        delay(2000) // suspend the main thread for a few seconds
        time!! `should be greater than` 1500
        time!! `should be less than` 2000
    }

    @Test
    fun `multi builder publishOn CommonPool`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var source = multi(coroutineContext) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
        // subscribe on another thread with a slow subscriber using Multi
        var start: Long? = null
        var time: Long? = null
        source = source
                .publishOn(Schedulers.commonPoolThreadContext(), false, 2) // specify buffer size of 2 items
                .doOnSubscribe {
                    start = System.currentTimeMillis()
                    println("starting timer")
                }
                .doOnComplete {
                    val end = System.currentTimeMillis()
                    time = end - start!!
                    println("Completed in $time ms")
                }

        source.subscribe{x ->
            Thread.sleep(500) // 500ms to process each item
            println("Processed $x")
        }

        delay(2000) // suspend the main thread for a few seconds
        time!! `should be greater than` 1500
        time!! `should be less than` 2000
    }

    @Test
    fun `multi builder publishOn emptyThreadContext 2 consumers`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var start: Long? = null
        var time: Long?
        val source = multi(coroutineContext) {
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