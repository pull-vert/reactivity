package kotlinx.coroutines.experimental.common.expect

actual class AtomicReference<V> actual constructor(private var value: V) {
    actual fun get(): V = value
    actual fun set(value: V) {
        this.value = value
    }
    actual fun lazySet(newValue: V) {
        this.value = newValue
    }
    actual fun compareAndSet(expect: V, update: V): Boolean {
        if (this.value === expect) {
            this.value = update
            return true
        }
        return false
    }
}

actual class AtomicLong actual constructor() {
    var value = 0L
    actual fun set(newValue: Long) {
        this.value = newValue
    }
    actual fun incrementAndGet(): Long {
        return this.value++
    }
}