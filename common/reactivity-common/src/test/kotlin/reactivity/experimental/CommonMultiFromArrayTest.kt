package reactivity.experimental

import kotlin.test.Test

class CommonMultiFromArrayTest : TestBase() {
    @Test
    fun multiFromArray() = runTest {
        val source = arrayOf("0", "58").toMulti() // an array of Strings
        // iterate over the source fully
        source.consumeEach { println(it) }
    }

    @Test
    fun multiFromIntArray() = runTest {
        val source = intArrayOf(1, 89, 4567).toMulti() // an array of Int
        // iterate over the source fully
        source.consumeEach { println(it) }
    }

    @Test
    fun multiFromCharArray() = runTest {
        val source = charArrayOf('1', '8', 'z').toMulti() // an array of Char
        // iterate over the source fully
        source.consumeEach { println(it) }
    }
}
