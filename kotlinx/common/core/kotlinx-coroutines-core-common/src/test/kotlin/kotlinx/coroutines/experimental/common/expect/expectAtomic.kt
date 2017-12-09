package kotlinx.coroutines.experimental.common.expect

expect class AtomicInteger() {
    fun get(): Int
    fun incrementAndGet(): Int
}

expect class AtomicBoolean() {
    fun get(): Boolean
    fun getAndSet(newValue: Boolean): Boolean
}