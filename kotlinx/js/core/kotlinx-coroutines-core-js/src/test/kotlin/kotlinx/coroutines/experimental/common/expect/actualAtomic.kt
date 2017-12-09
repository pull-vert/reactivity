package kotlinx.coroutines.experimental.common.expect

actual class AtomicInteger actual constructor() {
    var value = 0
    actual fun get(): Int {
        return this.value
    }
    actual fun incrementAndGet(): Int {
        return this.value++
    }
}

actual class AtomicBoolean actual constructor() {
    var value = false
    actual fun get(): Boolean {
        return this.value
    }
    actual fun getAndSet(newValue: Boolean): Boolean {
        val oldValue = this.value
        this.value = newValue
        return oldValue
    }
}