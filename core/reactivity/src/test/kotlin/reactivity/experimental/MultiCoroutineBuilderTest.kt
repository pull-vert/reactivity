package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import org.junit.Test
import kotlin.test.assertEquals

class MultiCoroutineBuilderTest: TestBase() {

    @Test
    fun `multi builder 2 consumers 1 slow consumer`() = runTest {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var start: Long? = null
        var time: Long?
        val source = multi(coroutineContext.toScheduler()) {
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
}