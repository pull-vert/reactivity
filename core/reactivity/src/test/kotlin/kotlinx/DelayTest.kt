package kotlinx

import kotlinx.coroutines.experimental.delay
import org.junit.Test
import reactivity.experimental.TestBase

class DelayTest: TestBase() {
    @Test
    fun `delay test`() = runTest {
        println("Let's naively sleep for 1 second")
        delay(100L)
        println("We're still in Test EDT!")
    }
}