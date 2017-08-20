package io.khttp2.internal.common

import java.util.function.Supplier

/**
 * Given an [supplier] function constructs a [Supplier] that returns values through the [Supplier]
 * provided by that function.
 * The values are evaluated lazily, and the sequence is potentially infinite.
 *
 */
inline fun <T> Supplier(crossinline supplier: () -> Supplier<T>): Supplier<T> = object : Supplier<T> {
    override fun get(): T = get()
}