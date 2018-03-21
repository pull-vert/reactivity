package reactivity.experimental

import kotlin.test.Test
import kotlin.test.assertEquals

class CommonSoloTest : TestBase() {
    @Test
    fun soloMap() = runTest {
        val value = 12.toSolo()
                .map { it.toString() }
                .await()
        println("testSoloMap : value = $value")
        assertEquals("12", value)
    }

    @Test
    fun soloDelay() = runTest {
        val value = 12.toSolo()
                .delay(10)
                .await()
        println("testSoloDelay : value = $value")
        assertEquals(12, value)
    }

    @Test
    fun soloConsumeUnique() = runTest {
        12.toSolo()
                .consumeUnique { assertEquals(12, it) }
    }

    @Test
    fun soloFlatMap() = runTest {
        val value = "123456789".toSolo()
                .flatMap { it.split(Regex.fromLiteral("")).filter { it.isNotBlank() }.toMulti() }
                .map {
                    println(it)
                    it.toInt()
                }
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("soloFlatMap : value = $value")
        assertEquals(12, value)
    }
}