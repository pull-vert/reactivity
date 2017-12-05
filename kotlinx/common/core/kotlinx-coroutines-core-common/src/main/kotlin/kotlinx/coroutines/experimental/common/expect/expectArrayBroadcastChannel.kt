package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.channels.AbstractSendChannel
import kotlinx.coroutines.experimental.common.channels.BroadcastChannel

expect class ArrayBroadcastChannel<E>(
        /**
         * Buffer capacity.
         */
        capacity: Int
) : AbstractSendChannel<E>, BroadcastChannel<E>