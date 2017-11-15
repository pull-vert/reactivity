package reactivity.experimental.common

expect fun <T> atomic(initial: T): AtomicRef<T>

expect class AtomicRef<T> internal constructor(value: T) {
    @Volatile
    var value: T
    fun lazySet(value: T)
    fun compareAndSet(expect: T, update: T): Boolean
    fun getAndSet(value: T): T
    override fun toString(): String

}