package sourceCollector

import benchmark.N
import benchmark.isGood
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class SourceCollectorTest {

    @Test
    fun testSourceCollectorSync() = runBlocking {
        val value = SourceCollector
                .range(1, N)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceCollectorSync : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

        @Test
    fun testSourceCollectorQuick7() = runBlocking {
        val value = SourceCollector
                .range(1, 10)
                .async7(newSingleThreadContext("testSourceCollectorQuick7"), buffer = 8)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceCollectorQuick7 : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    //    @Test
//    fun testSourceCollectorQuick5() = runBlocking {
//        val value = SourceCollector
//                .range(1, 10)
//                .async5(newSingleThreadContext("testSourceCollectorQuick5"), buffer = 8)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//        println("testSourceCollectorQuick5 : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(12, value)
//    }
//
    //    @Test
//    fun testSourceCollectorQuick() = runBlocking {
//        val value = SourceCollector
//                .range(1, 10)
//                .async(newSingleThreadContext("testSourceCollectorQuick"), buffer = 8)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//        println("testSourceCollectorQuick : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(12, value)
//    }
//
//    @Test
//    fun testSourceCollectorQuick2() = runBlocking {
//        val value = SourceCollector
//                .range(1, 10)
//                .async2(newSingleThreadContext("testSourceCollectorQuick2"), buffer = 8)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//        println("testSourceCollectorQuick2 : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(12, value)
//    }
//
//    @Test
//    @Ignore
//    fun testSourceCollectorQuick3() = runBlocking {
//        val value = SourceCollector
//                .range(1, 10)
//                .async3(newSingleThreadContext("testSourceCollectorQuick3"), buffer = 8)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//        println("testSourceCollectorQuick3 : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(12, value)
//    }
//
//    @Test
//    fun testSourceCollectorQuick4() = runBlocking {
//        val value = SourceCollector
//                .range(1, 10)
//                .async4(newSingleThreadContext("testSourceCollectorQuick4"), buffer = 8)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//        println("testSourceCollectorQuick4 : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(12, value)
//    }
//
    @Test
    fun testSourceCollectorAsync7() = runBlocking {
        val value = SourceCollector
                .range(1, N)
                .async7(newSingleThreadContext("testSourceCollectorAsync7"), buffer = 128)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
        println("testSourceCollectorAsync7 : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }
//
//    @Test
//    fun testSourceCollectorAsync6() = runBlocking {
//        val value = SourceCollector
//                .range(1, N)
//                .async6(newSingleThreadContext("testSourceCollectorAsync6"), buffer = 128)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//        println("testSourceCollectorAsync6 : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(446448416, value)
//    }
//
//    @Test
//    @Ignore
//    fun testSourceCollectorAsync() = runBlocking {
//        val value = SourceCollector
//                .range(1, N)
//                .async(newSingleThreadContext("testSourceCollectorAsync"), buffer = 64)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//        println("testSourceCollectorAsync : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(446448416, value)
//    }
//
//    @Test
//    @Ignore
//    fun testSourceCollectorAsync2() = runBlocking {
//        val value = SourceCollector
//                .range(1, N)
//                .async2(newSingleThreadContext("testSourceCollectorAsync2"), buffer = 128)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//        println("testSourceCollectorAsync2 : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(446448416, value)
//    }
//
//    @Test
//    @Ignore
//    fun testSourceCollectorAsync3() = runBlocking {
//        val value = SourceCollector
//                .range(1, N)
//                .async3(newSingleThreadContext("testSourceCollectorAsync3"), buffer = 128)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//        println("testSourceCollectorAsync3 : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(446448416, value)
//    }
//
//    @Test
//    @Ignore
//    fun testSourceCollectorAsync4() = runBlocking {
//        val value = SourceCollector
//                .range(1, N)
//                .async4(newSingleThreadContext("testSourceCollectorAsync4"), buffer = 128)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//        println("testSourceCollectorAsync4 : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(446448416, value)
//    }
//
//    @Test
//    @Ignore
//    fun testSourceCollectorAsync5() = runBlocking {
//        val value = SourceCollector
//                .range(1, N)
//                .async5(newSingleThreadContext("testSourceCollectorAsync5"), buffer = 128)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//        println("testSourceCollectorAsync5 : value = $value run on ${Thread.currentThread().name}")
//        assertEquals(446448416, value)
//    }
}