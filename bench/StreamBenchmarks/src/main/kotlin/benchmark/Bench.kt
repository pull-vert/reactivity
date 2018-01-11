package benchmark

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder



@State(Scope.Benchmark)
class Bench {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val options = OptionsBuilder()
                    //                .timeout(TimeValue.seconds(13))
                    .include(".*")
                    .warmupIterations(20)
                    .measurementIterations(20)
                    .forks(1)
                    .threads(355)
                    .mode(Mode.Throughput)
                    .resultFormat(ResultFormatType.JSON)
                    .result("build/jmh-result.json")
                    .shouldFailOnError(true)
                    .build()

            Runner(options).run()
        }
    }

    @Benchmark
    fun testBaselineLoop(): Int {
        var sum = 0
        for (i in 1..N) {
            if (i.isGood())
                sum += i
        }
        return sum
    }
}