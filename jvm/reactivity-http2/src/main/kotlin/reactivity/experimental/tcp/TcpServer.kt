package reactivity.experimental.tcp

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.nio.aAccept
import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import kotlinx.coroutines.experimental.withTimeout
import mu.KLogging
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import kotlin.coroutines.experimental.CoroutineContext

val PORT = 12345
val BACKLOG = 0
val CLIENT_READ_TIMEOUT = 30000L // 30 sec
val CLIENT_WRITE_TIMEOUT = 1000L // 1 sec
val BUFFER_SIZE = 1024

class TcpServer(hostname: String? = null,
                port: Int = PORT,
                backlog: Int = BACKLOG) {
    companion object: KLogging()

    val serverChannel = if (null != hostname) {
            AsynchronousServerSocketChannel
                    .open()
                    .bind(InetSocketAddress(hostname, port), backlog)
    } else {
        AsynchronousServerSocketChannel
                .open()
                .bind(InetSocketAddress(port), backlog)
    }

    suspend fun run(coroutineContext: CoroutineContext) {
        logger.info { "Listening on port $PORT" }
        // loop and accept connections forever
        while (true) {
            val client = serverChannel.aAccept()
            val address = try {
                val ia = client.remoteAddress as InetSocketAddress
                "${ia.address.hostAddress}:${ia.port}"
            } catch (ex: Throwable) {
                logger.warn(ex) {"Accepted client connection but failed to get its address because of $ex" }
                continue /* accept next connection */
            }
            logger.debug { "Accepted client connection from $address" }
            // just start a new coroutine for each client connection
            launch(coroutineContext) {
                try {
                    handleClient(client)
                    logger.debug { "Client connection from $address has terminated normally" }
                } catch (ex: Throwable) {
                        logger.warn(ex) { "Client connection from $address has terminated because of $ex" }
                }
            }
        }
    }

    private suspend fun handleClient(client: AsynchronousSocketChannel) {
        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
        while (true) {
            val bytes = withTimeout(CLIENT_READ_TIMEOUT) { client.aRead(buffer) }
            if (bytes < 0) break
            buffer.flip()
            logger.debug(String(buffer.array()))
//        withTimeout(CLIENT_WRITE_TIMEOUT) { client.aWrite(buffer) }
            buffer.clear()
        }
    }

    fun close() {
        logger.info { "Closing on port $PORT" }
        if (serverChannel.isOpen) serverChannel.close()
    }
}