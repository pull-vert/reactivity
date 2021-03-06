package sourceInline

import benchmark.N
import benchmark.isGood
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class SourceInlineTest {

    @Test
    fun testSourceInline() = runBlocking {
        val value = SourceInline
                .range(1, N)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInline : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun testSourceInlineQuick() = runBlocking {
        val value = SourceInline
                .range(1, 10)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInlineQuick : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    @Test
    fun testSourceInlineMpMc() = runBlocking {
        val value = SourceInline
                .range(1, N)
                .asyncMpMc(buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInlineMpMc : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun testSourceInlineSpSc() = runBlocking {
        val value = SourceInline
                .range(1, N)
                .asyncSpSc(buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInlineSpSc : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

//    @Test
//    fun testSourceInlineSpScQuick() = runBlocking {
//        val value = SourceInline
//                .range(1, 10)
//                .asyncSpSc(buffer = 128)
//                .filter2 { it.isGood() }
//                .fold2(0, { a, b -> a + b })
//        println("testSourceInlineSpScQuick : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(12, value)
//    }

    @Test
    fun testSourceInlineSpScLaunchSimpleQuasar() = runBlocking {
        val value = SourceInline
                .range(1, N)
                .asyncSpScLaunchSimple(buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceInlineSpScLaunchSimpleQuasar : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }
}