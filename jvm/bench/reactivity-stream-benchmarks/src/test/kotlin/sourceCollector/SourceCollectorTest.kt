package sourceCollector

import benchmark.N
import benchmark.isGood
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import reactivity.experimental.TestBase
import kotlin.test.assertEquals

class SourceCollectorTest: TestBase() {

    @Test
    fun testSourceCollectorSync() = runTest {
        val value = SourceCollector
                .range(1, 5)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceCollectorSync : value = $value run on ${Thread.currentThread().name}")
        assertEquals(4, value)
    }

    @Test
    fun testSourceCollectorQuick() = runBlocking {
        val value = SourceCollector
                .range(1, 10)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceCollectorQuick : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    @Test
    fun testSourceCollectorAsyncQuick() = runTest {
        val value = SourceCollector
                .range(1, 10)
                .async(buffer = 2)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceCollectorAsyncQuick : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    @Test
    fun testSourceCollectorAsync() = runBlocking {
        val value = SourceCollector
                .range(1, N)
                .async(buffer = 32)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceCollectorAsync : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }
}