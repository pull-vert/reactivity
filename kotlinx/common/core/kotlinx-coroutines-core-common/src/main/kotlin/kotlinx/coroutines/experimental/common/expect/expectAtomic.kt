package kotlinx.coroutines.experimental.common.expect

expect class AtomicReference<V>(value: V) {
    fun get(): V
    fun set(value: V)
    fun lazySet(newValue: V)
    fun compareAndSet(expect: V, update: V): Boolean
}

expect class AtomicLong() {
    fun set(newValue: Long)
    fun incrementAndGet(): Long
}