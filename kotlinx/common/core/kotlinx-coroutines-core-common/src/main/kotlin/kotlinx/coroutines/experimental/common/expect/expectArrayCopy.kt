package kotlinx.coroutines.experimental.common.expect

expect fun <T> arraycopy(src: Array<T>, srcPos: Int, dest: Array<T?>, destPos: Int, length: Int)