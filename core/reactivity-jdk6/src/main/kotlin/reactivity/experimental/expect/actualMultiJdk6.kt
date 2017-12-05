package reactivity.experimental.expect

import kotlinx.coroutines.experimental.reactive.consumeEach
import reactivity.experimental.*

actual fun <T> Publisher<T>.toMulti(): Multi<T> = multi(SCHEDULER_DEFAULT_DISPATCHER) {
    this@toMulti.consumeEach {
        send(it)
    }
}

actual fun <T> Publisher<T>.toMulti(scheduler: Scheduler): Multi<T> = multi(scheduler) {
    this@toMulti.consumeEach {
        send(it)
    }
}

actual abstract class Multi<T> : IMulti<T> {
    /**
     * Builder for [Multi], a multi values Reactive Stream [Publisher]
     *
     * @author Frédéric Montariol
     */
    companion object {
        // Static factory methods to create a Multi

        /**
         * Creates a [Multi] from a range of Int (starting from [start] and emmitting
         * [count] items)
         *
         * @return the [Multi]<Int> created
         */
        @JvmStatic
        fun fromRange(start: Int, count: Int): Multi<Int> = IntRange(start, start + count - 1).toMulti()

        /**
         * Creates a [Multi] from a range of Int (starting from [start] and emmitting
         * [count] items)
         *
         * @return the [Multi]<Int> created
         */
        @JvmStatic
        fun fromRange(scheduler: Scheduler, start: Int, count: Int) = IntRange(start, start + count - 1).toMulti(scheduler)

        /**
         * Creates a [Multi] from a [Iterable]
         *
         * @return the [Multi]<T> created
         *
         * @param T the type of the input [iterable]
         */
        @JvmStatic
        fun <T> fromIterable(iterable: Iterable<T>) = iterable.toMulti()

        /**
         * Creates a [Multi] from a [Iterable]
         *
         * @return the [Multi]<T> created
         *
         * @param T the type of the input [iterable]
         */
        @JvmStatic
        fun <T> fromIterable(scheduler: Scheduler, iterable: Iterable<T>) = iterable.toMulti(scheduler)

        /**
         * Creates a [Multi] from a [Array]
         *
         * @return the [Multi]<T> created
         *
         * @param T the type of the input [array]
         */
        @JvmStatic
        fun <T> fromArray(array: Array<T>) = array.toMulti()

        /**
         * Creates a [Multi] from a [Array]
         *
         * @return the [Multi]<T> created
         *
         * @param T the type of the input [array]
         */
        @JvmStatic
        fun <T> fromArray(scheduler: Scheduler, array: Array<T>) = array.toMulti(scheduler)

        /**
         * Creates a [Multi] from a [Publisher]
         *
         * @return the [Multi]<T> created
         *
         * @param T the type of the input [Publisher]
         */
        @JvmStatic
        fun <T> fromPublisher(publisher: Publisher<T>) = publisher.toMulti()

        /**
         * Creates a [Multi] from a [Publisher]
         *
         * *To notice : no need for [Multi] coroutine here !*
         *
         * @return the [Multi]<T> created
         *
         * @param T the type of the input [Publisher]
         */
        @JvmStatic
        fun <T> fromPublisher(scheduler: Scheduler, publisher: Publisher<T>) = publisher.toMulti(scheduler)
    }
}

actual abstract class AMulti<T> : Multi<T>(), IMultiImpl<T>