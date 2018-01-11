//package benchmark
//
//import org.junit.Test
//import org.openjdk.jmh.annotations.BenchJava
//import kotlin.system.measureTimeMillis
//import kotlin.test.assertEquals
//
//class RangeFilterSumTest {
//    @Test
//    fun testConsistentResults() {
//        val b = RangeFilterSumBenchmark()
//        val expected = b.testBaselineLoop()
//        var passed = 0
//        var failed = 0
//        for (m in b::class.java.methods) {
//            if (m.getAnnotation(BenchJava::class.java) != null) {
//                print("${m.name} = ")
//                val time = measureTimeMillis {
//                    val result = m.invoke(b) as Int
//                    print("$result ${if (result == expected) "[OK]" else "!!!FAILED!!!"}")
//                    if (result == expected) passed++ else failed++
//                }
//                println(" in $time ms")
//            }
//        }
//        println("PASSED: $passed")
//        println("FAILED: $failed")
//        assertEquals(0, failed)
//    }
//}