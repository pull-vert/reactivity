package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlin.test.Test
import kotlin.test.assertTrue

class CommonMultiPublishOnTest: TestBase() {

    @Test
    fun `multi builder publishOn slow producer`() = runTest {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var source = multi(coroutineContext.toScheduler()) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
                .delay(100) // delay every send

        // subscribe on another thread with a slow subscriber using Multi
        var start: Long? = null
        var time: Long? = null
        source = source
                .publishOn(SCHEDULER_DEFAULT_DISPATCHER, false, 2) // specify buffer size of 2 items
                .doOnNext {
                    if (start === null) {
                        start = currentTimeMillis()
                        println("starting timer")
                    }
                }
                .doOnComplete {
                    val end = currentTimeMillis()
                    time = end - start!!
                    println("Completed in $time ms")
                }

        source.subscribe()

        delay(400) // suspend the main thread for a few seconds
        assertTrue(time!! > 95)
        assertTrue(time!! < 200)
    }

    @Test
    fun `multi builder publishOn prefetch more than produced`() = runTest {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var source = multi(coroutineContext.toScheduler()) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }

        var completed: Boolean = false
        // subscribe on another thread with a slow subscriber using Multi
        source = source
                .publishOn(SCHEDULER_DEFAULT_DISPATCHER, false, 5) // specify buffer size of 5 items
                .doOnComplete {
                    println("Completed")
                    completed = true
                }

        source.subscribe()

        delay(100) // suspend the main thread for a few seconds
        assertTrue(completed)
    }
}