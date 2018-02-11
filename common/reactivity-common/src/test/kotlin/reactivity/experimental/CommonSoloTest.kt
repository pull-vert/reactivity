package reactivity.experimental

import kotlin.test.Test
import kotlin.test.assertEquals

class CommonSoloTest : TestBase() {
    @Test
    fun testSoloMap() = runTest {
        val value = 12.toSolo()
                .map { it.toString() }
                .await()
        println("testSoloMap : value = $value")
        assertEquals("12", value)
    }

    @Test
    fun testSoloDelay() = runTest {
        val value = 12.toSolo()
                .delay(10)
                .await()
        println("testSoloDelay : value = $value")
        assertEquals(12, value)
    }

    @Test
    fun testSoloConsumeUnique() = runTest {
        12.toSolo()
                .consumeUnique { assertEquals(12, it) }
    }
}