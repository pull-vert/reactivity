//package reactivity.experimental.tcp
//
//import kotlinx.coroutines.experimental.nio.aRead
//import kotlinx.coroutines.experimental.runBlocking
//import kotlinx.coroutines.experimental.withTimeout
//import mu.KotlinLogging
//import reactivity.experimental.CLIENT_READ_TIMEOUT
//import java.nio.ByteBuffer
//
//private val logger = KotlinLogging.logger {}
//
//object TcpServerMain {
//    @JvmStatic
//    fun main(args: Array<String>) = runBlocking {
//        TcpServer(doOnNewSocketChannel = { client ->
//            // will finally close the client
//            client.use {
//                val buffer = ByteBuffer.allocate(BUFFER_SIZE)
//                val bytes = withTimeout(CLIENT_READ_TIMEOUT) { it.aRead(buffer) }
//                buffer.flip()
//                logger.debug("Reading $bytes bytes : ${Charsets.UTF_8.decode(buffer)}")
//                buffer.clear()
//            }
//        }).launch(coroutineContext)
//    }
//}
