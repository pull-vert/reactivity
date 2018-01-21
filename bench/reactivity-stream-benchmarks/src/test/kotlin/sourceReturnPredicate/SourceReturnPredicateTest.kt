package sourceReturnPredicate

import benchmark.N
import benchmark.isGood
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Ignore
import org.junit.Test
import sourceSendOnly.*
import kotlin.test.assertEquals

class SourceReturnPredicateTest {

    @Test
    fun testSourceReturnPredicateSync() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, N)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateSync : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun testSourceReturnPredicateQuick() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, 10)
                .async(newSingleThreadContext("testSourceReturnPredicateQuick"), buffer = 8)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateQuick : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    @Test
    fun testSourceReturnPredicateQuick2() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, 10)
                .async2(newSingleThreadContext("testSourceReturnPredicateQuick2"), buffer = 8)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateQuick2 : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    @Test
    @Ignore
    fun testSourceReturnPredicateQuick3() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, 10)
                .async3(newSingleThreadContext("testSourceReturnPredicateQuick3"), buffer = 8)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateQuick3 : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    @Test
    fun testSourceReturnPredicateQuick4() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, 10)
                .async4(newSingleThreadContext("testSourceReturnPredicateQuick4"), buffer = 8)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateQuick4 : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    @Test
    @Ignore
    fun testSourceReturnPredicateAsync() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, N)
                .async(newSingleThreadContext("testSourceReturnPredicateAsync"), buffer = 64)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateAsync : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    @Ignore
    fun testSourceReturnPredicateAsync2() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, N)
                .async2(newSingleThreadContext("testSourceReturnPredicateAsync2"), buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateAsync2 : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    @Ignore
    fun testSourceReturnPredicateAsync3() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, N)
                .async3(newSingleThreadContext("testSourceReturnPredicateAsync3"), buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateAsync3 : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun testSourceReturnPredicateAsync4() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, N)
                .async4(newSingleThreadContext("testSourceReturnPredicateAsync4"), buffer = 128)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateAsync4 : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }
}