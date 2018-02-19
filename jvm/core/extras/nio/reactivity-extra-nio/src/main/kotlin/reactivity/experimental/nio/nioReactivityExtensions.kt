package reactivity.experimental.nio

import kotlinx.coroutines.experimental.nio.aRead
import reactivity.experimental.SCHEDULER_DEFAULT_DISPATCHER
import reactivity.experimental.Scheduler
import reactivity.experimental.solo
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit


// Solo
//@fixme This is just an example that will be used for reactivity-http2
fun AsynchronousSocketChannel.toSoloRead(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER,
                                         buf: ByteBuffer,
                                         timeout: Long = 0L,
                                         timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) = solo(scheduler) {
    val sb = StringBuilder()

    buf.clear()
    var read = this@toSoloRead.aRead(buf)
    while (read > 0) {
        buf.flip()
        // ready to use the bytes !!
//        val bytes = ByteArray(buf.limit())
//        buf.get(bytes)
//        sb.append(String(bytes))
        buf.clear()
        read = this@toSoloRead.aRead(buf)
    }
    // end of read, send the content (as Stream instance)
    send(sb.toString())
}
