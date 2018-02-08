package reactivity.experimental

import org.junit.Test
import reactivity.experimental.coroutine.coroutine
import reactivity.experimental.coroutine.delay
import kotlin.test.assertEquals

class MultiTest: TestBase() {

    @Test
    fun testMultiQuick() = runTest {
        val value = Multi
                .range(1, 10)
                .async(buffer = 8)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testMultiQuick : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    @Test
    fun testMultiMedium() = runTest {
        val value = Multi
                .range(1, 1_000)
                .async(buffer = 4)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testMultiMedium : value = $value run on ${Thread.currentThread().name}")
        assertEquals(125500, value) // 1_000
    }

    @Test
    fun testMulti() = runTest {
        val value = Multi
                .range(1, N)
                .async(buffer = 128)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testMulti : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun simpleCoroutineTest() = runTest {
        coroutine {
            val value = Multi
                    .range(1, 10)
                    .delay(10)
                    .filter { it.isGood() }
                    .fold(0, { a, b -> a + b })
            println("simpleCoroutineTest : value = $value run on ${Thread.currentThread().name}")
            assertEquals(12, value)
        }
        delay(200)
    }
}