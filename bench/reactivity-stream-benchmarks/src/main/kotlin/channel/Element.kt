package channel

/**
 * Element stored in the buffer
 */
data class Element<E : Any>(
        val item: E? = null,
        val closeCause: Throwable? = null
)