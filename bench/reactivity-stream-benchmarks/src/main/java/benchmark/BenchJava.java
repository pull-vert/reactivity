package benchmark;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class BenchJava extends RangeFilterSumBenchmark {

    public static void main(String... args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*")
                .forks(1)
                .jvmArgsAppend("-Dkotlinx.coroutines.debug=off")
                .warmupIterations(5)
//                .measurementIterations(10)
                .measurementIterations(5)
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MILLISECONDS)
                .resultFormat(ResultFormatType.JSON)
                .result("build/jmh-result.json")
                .shouldFailOnError(true)
                .build();

        new Runner(options).run();
    }
}
