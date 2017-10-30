package reactivity.experimental.swing

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should equal`
import org.junit.Test
import reactivity.experimental.jdk8.multiOld
import javax.swing.SwingUtilities

class SchedulerSwingTest {
    @Test
    fun `multi builder swing context`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var start: Long? = null
        var time: Long?
        val source = multiOld(SCHEDULER_SWING_DISPATCHER) {
            for (x in 1..3) {
                check(SwingUtilities.isEventDispatchThread())
                delay(100)
                check(SwingUtilities.isEventDispatchThread())
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
        delay(500) // suspend the main thread for a few seconds
        count `should equal` 3
    }
}