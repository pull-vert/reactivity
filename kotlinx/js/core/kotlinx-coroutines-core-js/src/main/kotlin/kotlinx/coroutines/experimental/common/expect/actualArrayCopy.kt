package kotlinx.coroutines.experimental.common.expect

actual fun <T> arraycopy(src: Array<T>, srcPos: Int, dest: Array<T?>, destPos: Int, length: Int) {
    // see https://stackoverflow.com/questions/15501251/efficient-javascript-equivalent-of-processing-functions
    var localLength = length
    var localDestPos = destPos
    localLength += srcPos
    localDestPos += length
    while(--localLength > srcPos) {
        dest[--localDestPos] = src[length]
    }
}