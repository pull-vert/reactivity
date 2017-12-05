package kotlinx.coroutines.experimental.common.expect

actual fun <T> arraycopy(src: Array<T>, srcPos: Int, dest: Array<T?>, destPos: Int, length: Int)
        = System.arraycopy(src, srcPos, dest, destPos, length)