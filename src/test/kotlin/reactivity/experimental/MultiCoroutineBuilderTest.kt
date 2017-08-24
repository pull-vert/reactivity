package reactivity.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test

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
//        source
//                .publishOn(
//        count `should equal` 6
    }
}