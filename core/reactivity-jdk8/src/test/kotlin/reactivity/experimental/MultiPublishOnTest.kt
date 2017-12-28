package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

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

        source.subscribe {
            Thread.sleep(500) // 500ms to process each item
            println("Processed $it")
        }

        delay(2000) // suspend the main thread for a few seconds
        assertTrue(time!! > 1499)
        assertTrue(time!! < 2000)
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

        source.subscribe { x ->
            Thread.sleep(500) // 500ms to process each item
            println("Processed $x")
        }

        delay(2000) // suspend the main thread for a few seconds
        assertTrue(time!! > 1499)
        assertTrue(time!! < 2000)
    }

    @Test
    fun `multi builder publishOn slow producer`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var source = multi(schedulerFromCoroutineContext(coroutineContext)) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
                .delay(100, TimeUnit.MILLISECONDS) // delay every send

        // subscribe on another thread with a slow subscriber using Multi
        var start: Long? = null
        var time: Long? = null
        source = source
                .publishOn(SCHEDULER_COMMON_POOL_DISPATCHER, false, 2) // specify buffer size of 2 items
                .doOnNext {
                    if (start === null) {
                        start = System.currentTimeMillis()
                        println("starting timer")
                    }
                }
                .doOnComplete {
                    val end = System.currentTimeMillis()
                    time = end - start!!
                    println("Completed in $time ms")
                }

        source.subscribe()

        delay(400) // suspend the main thread for a few seconds
        assertTrue(time!! > 100)
        assertTrue(time!! < 200)
    }

    @Test
    fun `multi builder publishOn prefetch more than produced`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var source = multi(schedulerFromCoroutineContext(coroutineContext)) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }

        var completed: Boolean = false
        // subscribe on another thread with a slow subscriber using Multi
        source = source
                .publishOn(SCHEDULER_COMMON_POOL_DISPATCHER, false, 5) // specify buffer size of 5 items
                .doOnComplete {
                    println("Completed")
                    completed = true
                }

        source.subscribe()

        delay(100) // suspend the main thread for a few seconds
        assertTrue(completed)
    }
}