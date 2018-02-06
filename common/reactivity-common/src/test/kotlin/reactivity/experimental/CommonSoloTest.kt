package reactivity.experimental

import kotlin.test.Test
import kotlin.test.assertEquals

class CommonSoloTest : TestBase() {
    @Test
    fun testSoloMap() = runTest {
        val value = Solo
                .fromValue(12)
                .map { it.toString() }
                .await()
        println("testSoloMap : value = $value")
        assertEquals("12", value)
    }

    @Test
    fun testSoloDelay() = runTest {
        val value = Solo
                .fromValue(12)
                .delay(100) // 100ms delay for the item
                .await()
        println("testSoloDelay : value = $value")
        assertEquals(12, value)
    }
}