package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonMultiCoroutineBuilderTest : TestBase() {

    @Test
    fun `multi builder 2 consumers`() = runTest {
        // coroutine -- fast producer of elements in the context of the main thread (= coroutineContext)
        var start: Long? = null
        var time: Long?
        val source = multi(schedulerFromCoroutineContext(coroutineContext)) {
            for (x in 1..3) {
                send(x) // this is a suspending function
                println("Sent $x") // print after successfully sent item
            }
        }
                .doOnSubscribe {
                    start = currentTimeMillis()
                }
                .doOnComplete {
                    val end = currentTimeMillis()
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
            println("second consumer: Processed $x")
            count++
        }
        delay(50) // suspend the main thread for a few seconds
        assertEquals(6, count)
    }
}