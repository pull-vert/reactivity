package reactivity.experimental

import kotlin.test.Test
import kotlin.test.assertEquals

const val N = 1_000_000

fun Int.isGood() = this % 4 == 0

class CommonMultiTest : TestBase() {
    @Test
    fun testMulti() = runTest {
        val value = Multi
                .range(1, N)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testMulti : value = $value")
        assertEquals(446448416, value)
    }

    @Test
    fun testMultiMap() = runTest {
        val value = Multi
                .range(1, 2)
                .map { it.toString() }
                .fold("", { a, b -> a + b })
        println("testMultiMap : value = $value")
        assertEquals("12", value)
    }

    @Test
    fun testMultiDelay() = runTest {
        val value = Multi
                .range(1, 10)
                .delay(100) // 100ms delay for each item
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testMultiDelay : value = $value")
        assertEquals(12, value)
    }
}