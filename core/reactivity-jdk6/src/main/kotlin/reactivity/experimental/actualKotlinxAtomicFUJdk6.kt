package reactivity.experimental

import kotlinx.atomicfu.AtomicRef

actual fun <T> atomic(initial: T): AtomicRef<T> = kotlinx.atomicfu.atomic(initial)

actual typealias AtomicRef<T> = AtomicRef<T>