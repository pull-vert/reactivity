package reactivity.experimental

import kotlin.test.Test
import kotlin.test.assertEquals

const val N = 1_000_000

fun Int.isGood() = this % 4 == 0

class CommonMultiTest : TestBase() {
    @Test
    fun multiFilterThenFold() = runTest {
        val value = Multi
                .range(1, N)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("multiFilterThenFold : value = $value")
        assertEquals(446448416, value)
    }

    @Test
    fun multiMap() = runTest {
        val value = Multi
                .range(1, 2)
                .map { it.toString() }
                .fold("", { a, b -> a + b })
        println("multiMap : value = $value")
        assertEquals("12", value)
    }

    @Test
    fun multiDelay() = runTest {
        val value = Multi
                .range(1, 10)
                .delayEach(10)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("multiDelay : value = $value")
        assertEquals(12, value)
    }

    @Test
    fun multiFromSequenceAndConsumeEach() = runTest {
        sequenceOf("lower", "case").toMulti()
                .map { it.toUpperCase() }
                // iterate over the source fully
                .consumeEach { println(it) }
    }

    @Test
    fun multiFirst() = runTest {
        val value = sequenceOf("lower", "case").toMulti()
                .first()
                // iterate over the source fully
                .await()
        println("multiFirst : value = $value")
        assertEquals("lower", value)
    }

    @Test
    fun multiReduce() = runTest {
        val value = Multi
                .range(1, 10)
                .filter { it.isGood() }
                .reduce(0, { a, b -> a + b })
                .await()
        println("multiReduce : value = $value")
        assertEquals(12, value)
    }
}