package reactivity.experimental.javafx

import javafx.application.Platform
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import reactivity.experimental.multi
import kotlin.test.assertEquals

class SchedulerJavaFxTest {
    @Test
    fun `multi builder JavaFx context`() = runBlocking {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var start: Long? = null
        var time: Long?
        val source = multi(SCHEDULER_JAVA_FX_DISPATCHER) {
            for (x in 1..3) {
                check(Platform.isFxApplicationThread())
                delay(100)
                check(Platform.isFxApplicationThread())
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
        source.subscribe { x ->
            println("first consumer: Processed $x")
            count++
        }
        delay(500) // suspend the main thread for a few seconds
        assertEquals(3, count)
    }
}