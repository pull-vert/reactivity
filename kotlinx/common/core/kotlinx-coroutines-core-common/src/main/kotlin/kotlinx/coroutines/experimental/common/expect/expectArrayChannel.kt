package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.channels.AbstractChannel

expect open class ArrayChannel<E>(
        /**
         * Buffer capacity.
         */
        capacity: Int
) : AbstractChannel<E>