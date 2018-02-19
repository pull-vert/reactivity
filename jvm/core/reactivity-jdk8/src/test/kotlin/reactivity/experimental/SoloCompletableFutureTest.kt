package reactivity.experimental

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.future.future
import org.junit.Test
import reactivity.experimental.jdk8.toCompletableFuture
import reactivity.experimental.jdk8.toSolo
import kotlin.test.assertEquals

class SoloCompletableFutureTest: TestBase() {
    @Test
    fun `Solo from CompletableFuture`() = runTest {
        val value = future { 12 }
                .toSolo()
                .await()
        println("Solo from CompletableFuture : value = $value")
        assertEquals(12, value)
    }

    @Test
    fun `Solo to CompletableFuture`() = runTest {
        val value = 12
                .toSolo()
                .toCompletableFuture()
        value.thenAcceptAsync {
            println("Solo to CompletableFuture : value = $it")
            assertEquals(12, it)
        }
        delay(50) // wait for the future to finish
    }
}