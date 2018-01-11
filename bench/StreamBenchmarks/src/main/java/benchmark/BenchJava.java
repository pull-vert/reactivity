package benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@State(Scope.Benchmark)
public class BenchJava {

    Integer N = 1_000_000;

    public static void main(String... args) throws RunnerException {
        Options options = new OptionsBuilder()
//                .timeout(TimeValue.seconds(13))
                .include(".*")
                .forks(2)
                .jvmArgsAppend("-Dkotlinx.coroutines.debug=off")
                .warmupIterations(5)
                .measurementIterations(10)
//                .threads(355)
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MILLISECONDS)
                .resultFormat(ResultFormatType.JSON)
                .result("build/jmh-result.json")
                .shouldFailOnError(true)
                .build();

        new Runner(options).run();
    }

    @Benchmark
    public Integer testBaselineLoop() {
        Integer sum = 0;
        for (int i = 0; i < N; i++) {
            if (isGood(i))
                sum += i;
        }
        return sum;
    }

    private boolean isGood(Integer i) {
        return i % 4 == 0;
    }

    @Benchmark
    public Integer testJavaStream() {
        return Stream.iterate(1, n -> n + 1)
            .limit(N.longValue())
            .filter(this::isGood)
            .collect(Collectors.summingInt(n -> n));
    }

//    @Benchmark
//    fun testObservable(): Int =
//            Observable
//                    .range(1, N)
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v

//    @Benchmark
//    fun testFlowable(): Int =
//            Flowable
//                    .range(1, N)
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFlux(): Int =
//            Flux
//                    .range(1, N)
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .block()!!.v
//
//    @Benchmark
//    fun testObservableThread(): Int =
//            Observable
//                    .range(1, N)
//            .observeOn(Schedulers.computation())
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFlowableThread(): Int =
//            Flowable
//                    .range(1, N)
//            .observeOn(Schedulers.computation())
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFluxThread(): Int =
//            Flux
//                    .range(1, N)
//            .publishOn(reactor.core.scheduler.Schedulers.single())
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .block()!!.v
}
