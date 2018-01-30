package reactivity.experimental.channel

internal const val DEFAULT_CLOSE_MESSAGE = "SpScChannel was closed"

/**
 * Element stored in the buffer
 */
data class Element<E : Any>(
        val item: E? = null,
        val closeCause: Throwable? = null
)