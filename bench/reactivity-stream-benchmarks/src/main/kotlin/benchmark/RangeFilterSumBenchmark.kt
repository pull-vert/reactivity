package benchmark

import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.reactive.publish
import kotlinx.coroutines.experimental.reactor.flux
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.rx2.consumeEach
import kotlinx.coroutines.experimental.rx2.rxFlowable
import kotlinx.coroutines.experimental.rx2.rxObservable
import org.openjdk.jmh.annotations.Benchmark
import org.reactivestreams.Publisher
import reactivity.experimental.channel.SourceInline
import reactivity.experimental.channel.filter2
import reactivity.experimental.channel.fold2
import reactivity.experimental.channel.range
import reactor.core.publisher.Flux
import source.*
import sourceSendOnly.*
import srcmanbase.*
import suspendingSequence.SuspendingSequence
import suspendingSequence.suspendingSequence
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.buildSequence

fun Int.isGood() = this % 4 == 0

fun Channel.Factory.range(start: Int, count: Int, context: CoroutineContext = DefaultDispatcher) =
        produce<Int>(context) {
            for (i in start until (start + count))
                send(i)
        }

fun publishRange(start: Int, count: Int, context: CoroutineContext = DefaultDispatcher) =
        publish<Int>(context) {
            for (i in start until (start + count))
                send(i)
        }

fun sequenceGenerateRange(start: Int, count: Int): Sequence<Int> {
    var cur = start
    return generateSequence {
        if (cur > start + count) {
            null
        } else {
            cur++
        }
    }
}

fun sequenceBuildRange(start: Int, count: Int) = buildSequence {
    for (i in start until (start + count))
        yield(i)
}

fun <T> Observable<T>.coroutineFilter(predicate: (T) -> Boolean) = rxObservable {
    consumeEach {
        if (predicate(it)) send(it)
    }
}

fun <T> Flowable<T>.coroutineFilter(predicate: (T) -> Boolean) = rxFlowable {
    consumeEach {
        if (predicate(it)) send(it)
    }
}

fun <T> Flux<T>.coroutineFilter(predicate: (T) -> Boolean) = flux {
    consumeEach {
        if (predicate(it)) send(it)
    }
}

fun <E> Publisher<E>.filter(predicate: suspend (E) -> Boolean) = publish<E> {
    consumeEach {
        if (predicate(it)) send(it)
    }
}

suspend fun <E, R> Publisher<E>.fold(initial: R, operation: suspend (acc: R, E) -> R): R {
    var acc = initial
    consumeEach {
        acc = operation(acc, it)
    }
    return acc
}

fun suspendingSequenceRange(start: Int, count: Int, context: CoroutineContext = DefaultDispatcher) =
        suspendingSequence<Int>(context) {
            for (i in start until (start + count))
                yield(i)
        }

fun <E> SuspendingSequence<E>.filter(predicate: suspend (E) -> Boolean) = suspendingSequence<E> {
    for (value in this@filter) {
        if (predicate(value)) yield(value)
    }
}

suspend fun <E, R> SuspendingSequence<E>.fold(initial: R, operation: suspend (acc: R, E) -> R): R {
    var acc = initial
    for (value in this@fold) {
        acc = operation(acc, value)
    }
    return acc
}

data class IntBox(var v: Int)

const val N = 1_000_000

open class RangeFilterSumBenchmark {
//    @Benchmark
//    fun testBaselineLoop(): Int {
//        var sum = 0
//        for (i in 1..N) {
//            if (i.isGood())
//                sum += i
//        }
//        return sum
//    }
//
//
//    @Benchmark
//    fun testSequenceIntRange(): Int =
//        (1..N)
//            .filter { it.isGood() }
//            .fold(0, { a, b -> a + b })
//
//    @Benchmark
//    fun testSequenceGenerate(): Int =
//        sequenceGenerateRange(1, N)
//            .filter { it.isGood() }
//            .fold(0, { a, b -> a + b })
//
//    @Benchmark
//    fun testSequenceBuild(): Int =
//        sequenceBuildRange(1, N)
//            .filter { it.isGood() }
//            .fold(0, { a, b -> a + b })
//
//    @Benchmark
//    fun testObservable(): Int =
//        Observable
//            .range(1, N)
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFlowable(): Int =
//        Flowable
//            .range(1, N)
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFlux(): Int =
//        Flux
//            .range(1, N)
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .block()!!.v
//
//    @Benchmark
//    fun testObservableThread(): Int =
//        Observable
//            .range(1, N)
//            .observeOn(Schedulers.computation())
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFlowableThread(): Int =
//        Flowable
//            .range(1, N)
//            .observeOn(Schedulers.computation())
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFluxThread(): Int =
//        Flux
//            .range(1, N)
//            .publishOn(reactor.core.scheduler.Schedulers.single())
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .block()!!.v
//
//
//    @Benchmark
//    fun testObservableFromCoroutinePublish(): Int =
//        Observable
//            .fromPublisher(publishRange(1, N))
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testObservableFromCoroutinePublishUnconfined(): Int =
//        Observable
//            .fromPublisher(publishRange(1, N, Unconfined))
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFlowableFromCoroutinePublish(): Int =
//        Flowable
//            .fromPublisher(publishRange(1, N))
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFlowableFromCoroutinePublishUnconfined(): Int =
//        Flowable
//            .fromPublisher(publishRange(1, N, Unconfined))
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFluxFromCoroutinePublish(): Int =
//        Flux
//            .from(publishRange(1, N))
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .block()!!.v
//
//    @Benchmark
//    fun testFluxFromCoroutinePublishUnconfined(): Int =
//        Flux
//            .from(publishRange(1, N, Unconfined))
//            .filter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .block()!!.v
//
//    @Benchmark
//    fun testObservableWithCoroutineFilter(): Int =
//        Observable
//            .range(1, N)
//            .coroutineFilter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFlowableWithCoroutineFilter(): Int =
//        Flowable
//            .range(1, N)
//            .coroutineFilter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .blockingGet().v
//
//    @Benchmark
//    fun testFluxWithCoroutineFilter(): Int =
//        Flux
//            .range(1, N)
//            .coroutineFilter { it.isGood() }
//            .collect({ IntBox(0) }, { b, x -> b.v += x })
//            .block()!!.v
//
//    @Benchmark
//    fun testChannelPipeline(): Int = runBlocking {
//        Channel
//            .range(1, N)
//            .filter { it.isGood() }
//            .fold(0, { a, b -> a + b })
//    }
//
//    @Benchmark
//    fun testChannelPipelineUnconfined(): Int = runBlocking {
//        Channel
//            .range(1, N, Unconfined)
//            .filter(Unconfined) { it.isGood() }
//            .fold(0, { a, b -> a + b })
//    }
//
//
//    @Benchmark
//    fun testSourceThread(): Int = runBlocking {
//        Source
//            .range(1, N)
//            .async()
//            .filter { it.isGood() }
//            .fold(0, { a, b -> a + b })
//    }
//
//
//    @Benchmark
//    fun testSource(): Int = runBlocking {
//        Source
//            .range(1, N)
//            .filter{ it.isGood() }
//            .fold(0, { a, b -> a + b })
//    }
//
//    @Benchmark
//    fun testSourceInline(): Int = runBlocking {
//        range(1, N)
//            .filter { it.isGood() }
//            .fold(0, { a, b -> a + b })
//    }
//

//    @Benchmark
//    fun testSourceInlineDeepFused(): Int = runBlocking {
//        SourceInline
//                .range(1, N)
//                .filterFold2(0, { a -> a.isGood() }, { a, b -> a + b })
//    }
//
//        @Benchmark
//    fun testJavaStream(): Int =
//        Stream
//            .iterate(1) { it + 1 }
//            .limit(N.toLong())
//            .filter { it.isGood() }
//            .collect(Collectors.summingInt { it })

    @Benchmark
    fun testSourceThreadBuffer128ArrayChannel(): Int = runBlocking {
        Source
                .range(1, N)
                .async(buffer = 128)
                .filter { it.isGood() }
                .fold(0, { a, b -> a + b })
    }

    @Benchmark
    fun testSourceReturnPredicate(): Int = runBlocking {
        SourceReturnPredicate
                .range(1, N)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
    }

//    @Benchmark
//    fun testSourceReturnPredicateThreadBuffer128SpScChannel(): Int = runBlocking {
//        SourceReturnPredicate
//                .range(1, N)
//                .async(newSingleThreadContext("test"), buffer = 128)
//                .filter2 { it.isGood() }
//                .fold2(0, { a, b -> a + b })
//    }

    @Benchmark
    fun testSourceReturnPredicateThreadBuffer128SpScChannel2(): Int = runBlocking {
        SourceReturnPredicate
                .range(1, N)
                .async2(newSingleThreadContext("test"), buffer = 256)
                .filter2 { it.isGood() }
                .fold2(0, { a, b -> a + b })
    }
//
//    @Benchmark
//    fun testSrcManBase(): Int = SrcManBase.noSuspend { cont ->
//        SrcManBase
//                .range(1, N)
//                .filter { it, _ -> it.isGood() }
//                .fold(0, { a, b, _ -> a + b }, cont)
//    }
//
//    @Benchmark
//    fun testMulti(): Int = runBlocking {
//        MultiBuilder
//                .fromRange(1, N)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//    }
//
//    @Benchmark
//    fun testPublish(): Int = runBlocking {
//        publishRange(1, N)
//                .filter{ it.isGood() }
//                .fold(0, { a, b -> a + b })
//    }
//
//    @Benchmark
//    fun testSuspendingSequence(): Int = runBlocking {
//        suspendingSequenceRange(1, N)
//                .filter { it.isGood() }
//                .fold(0, { a, b -> a + b })
//    }
}
