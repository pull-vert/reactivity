//package reactivity.experimental
//
//import kotlinx.coroutines.experimental.nio.aRead
//import kotlinx.coroutines.experimental.nio.aWrite
//import kotlinx.coroutines.experimental.withTimeout
//import java.nio.ByteBuffer
//import java.nio.channels.AsynchronousSocketChannel
//import java.util.concurrent.TimeUnit
//
//val CLIENT_READ_TIMEOUT = 30000L // 30 sec
//val CLIENT_WRITE_TIMEOUT = 30000L // 30 sec
//val TIMEOUT_UNIT = TimeUnit.MILLISECONDS // Base timeout TimeUnit
//
//suspend fun AsynchronousSocketChannel.aReadWithTimeout(
//        buffer: ByteBuffer,
//        timeout: Long = CLIENT_READ_TIMEOUT,
//        timeUnit: TimeUnit = TIMEOUT_UNIT
//) = withTimeout(timeout, timeUnit) { this@aReadWithTimeout.aRead(buffer) }
//
//suspend fun AsynchronousSocketChannel.aWriteWithTimeout(
//        buffer: ByteBuffer,
//        timeout: Long = CLIENT_WRITE_TIMEOUT,
//        timeUnit: TimeUnit = TIMEOUT_UNIT
//) = withTimeout(timeout, timeUnit) { this@aWriteWithTimeout.aWrite(buffer) }