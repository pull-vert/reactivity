package kotlinx

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test

class DelayTest {
    @Test
    fun `delay test`() = runBlocking {
        println("Let's naively sleep for 1 second")
        delay(100L)
        println("We're still in Test EDT!")
    }
}