package reactivity.experimental

import kotlinx.atomicfu.AtomicRef

actual typealias AtomicRef<T> = AtomicRef<T>

actual fun <T> atomic(initial: T): AtomicRef<T> = kotlinx.atomicfu.atomic(initial)