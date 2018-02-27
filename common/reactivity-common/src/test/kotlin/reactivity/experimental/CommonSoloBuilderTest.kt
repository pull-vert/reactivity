package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonSoloBuilderTest : TestBase() {
    @Test
    fun soloNoConsumer() = runTest {
        var value: String? = null
        solo(coroutineContext) {
                println("will send one !")
                value = "one"
                "one"
        }
        delay(20)
        assertEquals(null, value)
    }

    @Test
    fun soloWithConsumer() = runTest {
        var value: String? = null
        solo(coroutineContext) {
            println("will send one !")
            value = "one"
            "one"
        }.consumeUnique {
            println("consumeUnique $it")
        }
        delay(20)
        assertEquals("one", value)
    }
}