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
    fun testSourceReturnPredicateQuick() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, 10)
                .async(newSingleThreadContext("test"), buffer = 8)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateQuick : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    @Test
    fun testSourceReturnPredicateQuick2() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, 10)
                .async2(newSingleThreadContext("test2"), buffer = 8)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateQuick2 : value = $value run on ${Thread.currentThread().name}")
        assertEquals(12, value)
    }

    @Test
    fun testSourceReturnPredicate() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, N)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicate : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun testSourceReturnPredicateAsync() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, N)
                .async(newSingleThreadContext("test3"), buffer = 64)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateAsync : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }

    @Test
    fun testSourceReturnPredicateAsync2() = runBlocking {
        val value = SourceReturnPredicate
                .range(1, N)
                .async2(newSingleThreadContext("test4"), buffer = 64)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
        println("testSourceReturnPredicateAsync2 : value = $value run on ${Thread.currentThread().name}")
        assertEquals(446448416, value)
    }
}