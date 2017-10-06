package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should be greater than`
import org.amshove.kluent.`should be less than`
import org.junit.Test
import reactivity.experimental.core.SCHEDULER_COMMON_POOL_DISPATCHER
import reactivity.experimental.core.SCHEDULER_EMPTY_CONTEXT
import reactivity.experimental.core.schedulerFromCoroutineContext

class MultiPublishOnTest {
    @Test
    fun `multi builder publishOn emptyThreadContext`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var source = multi(schedulerFromCoroutineContext(coroutineContext)) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
        // subscribe on another thread with a slow subscriber using Multi
        var start: Long? = null
        var time: Long? = null
        source = source
                .publishOn(SCHEDULER_EMPTY_CONTEXT, false, 2) // specify buffer size of 2 items
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
        var source = multi(schedulerFromCoroutineContext(coroutineContext)) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
        // subscribe on another thread with a slow subscriber using Multi
        var start: Long? = null
        var time: Long? = null
        source = source
                .publishOn(SCHEDULER_COMMON_POOL_DISPATCHER, false, 2) // specify buffer size of 2 items
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
}