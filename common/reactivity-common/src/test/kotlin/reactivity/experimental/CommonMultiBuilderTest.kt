package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonMultiBuilderTest : TestBase() {
    @Test
    fun multiNoConsumer() = runTest {
        var value: String? = null
        multi(coroutineContext) {
            var cause: Throwable? = null
            try {
                println("will send one !")
                value = "one"
                send("one")
            } catch (e: Throwable) {
                cause = e
            }
            close(cause)
        }
        delay(20)
        assertEquals(null, value)
    }

    @Test
    fun multiWithConsumer() = runTest {
        var value: String? = null
        multi(coroutineContext) {
            var cause: Throwable? = null
            try {
                println("will send one !")
                value = "one"
                send("one")
            } catch (e: Throwable) {
                cause = e
            }
            close(cause)
        }.consumeEach {
            println("consume $it")
        }
        delay(20)
        assertEquals("one", value)
    }
}