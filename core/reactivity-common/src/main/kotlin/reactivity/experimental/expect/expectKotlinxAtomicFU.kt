package reactivity.experimental.expect

expect class AtomicRef<T> {
    @Volatile
    var value: T
    fun compareAndSet(expect: T, update: T): Boolean
    fun getAndSet(value: T): T
}

expect fun <T> atomic(initial: T): AtomicRef<T>