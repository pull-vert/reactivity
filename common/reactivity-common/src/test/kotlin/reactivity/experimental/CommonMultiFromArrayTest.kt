package reactivity.experimental

import kotlin.test.Test

class CommonMultiFromArrayTest : TestBase() {
    @Test
    fun multiFromArray() = runTest {
        val source = arrayOf("0", "58").toMulti()
        // iterate over the source fully
        source.consumeEach { println(it) }
    }

    @Test
    fun multiFromIntArray() = runTest {
        val source = intArrayOf(1, 89, 4567).toMulti()
        // iterate over the source fully
        source.consumeEach { println(it) }
    }

    @Test
    fun multiFromCharArray() = runTest {
        val source = charArrayOf('1', '8', 'z').toMulti()
        // iterate over the source fully
        source.consumeEach { println(it) }
    }

    @Test
    fun multiFromBooleanArray() = runTest {
        val source = booleanArrayOf(true, false).toMulti()
        // iterate over the source fully
        source.consumeEach { println(it) }
    }

    @Test
    fun multiFromByteArray() = runTest {
        val source = byteArrayOf(1, 2, 0, 9).toMulti()
        // iterate over the source fully
        source.consumeEach { println(it) }
    }

    @Test
    fun multiFromFloatArray() = runTest {
        val source = floatArrayOf(1f, 2.5f).toMulti()
        // iterate over the source fully
        source.consumeEach { println(it) }
    }

    @Test
    fun multiFromShortArray() = runTest {
        val source = shortArrayOf(1, 2, 12500).toMulti()
        // iterate over the source fully
        source.consumeEach { println(it) }
    }
}
