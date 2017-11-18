package reactivity.experimental

actual abstract class Solo<T> : ISolo<T> {
    /**
     * Builder for [Solo], a single (or empty) value Reactive Stream [Publisher]
     *
     * @author Frédéric Montariol
     */
    companion object {
        // protected Static factory methods to create a Solo

        /**
         * Creates a [Solo] from a [value]
         *
         * @return the [Solo]<T> created
         *
         * @param T the type of the input [value]
         */
        @JvmStatic
        fun <T> fromValue(value: T) = value.toSolo()

        /**
         * Creates a [Solo] from a [value]
         *
         * @return the [Solo]<T> created
         *
         * @param T the type of the input [value]
         */
        @JvmStatic
        fun <T> fromValue(scheduler: Scheduler, value: T) = value.toSolo(scheduler)
    }
}

actual abstract class ASolo<T> : Solo<T>(), ISoloImpl<T>