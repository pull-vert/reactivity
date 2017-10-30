package reactivity.experimental.jdk8

import org.reactivestreams.Publisher
import reactivity.experimental.Multi
import reactivity.experimental.Scheduler
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

/**
 * Jdk8 Builder for [Multi], a multi values Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
object MultiJdk8Builder {

    /**
     * Creates a [Multi] from a [Stream]
     *
     * @return the [Multi]<T> created
     *
     * @param T the type of the input [stream]
     */
    @JvmStatic
    fun <T> fromStream(stream: Stream<T>) = stream.toMulti()

    /**
     * Creates a [Multi] from a [Stream]
     *
     * @return the [Multi]<T> created
     *
     * @param T the type of the input [stream]
     */
    @JvmStatic
    fun <T> fromStream(scheduler: Scheduler, stream: Stream<T>) = stream.toMulti(scheduler)

    /**
     * Creates a [Multi] from a [IntStream]
     *
     * @return the [Multi]<Int> created
     */
    @JvmStatic
    fun fromIntStream(stream: IntStream) = stream.toMulti()

    /**
     * Creates a [Multi] from a [IntStream]
     *
     * @return the [Multi]<Int> created
     */
    @JvmStatic
    fun fromIntStream(scheduler: Scheduler, stream: IntStream) = stream.toMulti(scheduler)

    /**
     * Creates a [Multi] from a [LongStream]
     *
     * @return the [Multi]<Long> created
     */
    @JvmStatic
    fun fromLongStream(stream: LongStream) = stream.toMulti()

    /**
     * Creates a [Multi] from a [LongStream]
     *
     * @return the [Multi]<Long> created
     */
    @JvmStatic
    fun fromLongStream(scheduler: Scheduler, stream: LongStream) = stream.toMulti(scheduler)

    /**
     * Creates a [Multi] from a [DoubleStream]
     *
     * @return the [Multi]<Double> created
     */
    @JvmStatic
    fun fromDoubleStream(stream: DoubleStream) = stream.toMulti()

    /**
     * Creates a [Multi] from a [DoubleStream]
     *
     * @return the [Multi]<Double> created
     */
    @JvmStatic
    fun fromDoubleStream(scheduler: Scheduler, stream: DoubleStream) = stream.toMulti(scheduler)
}