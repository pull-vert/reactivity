package reactivity.experimental.tcp

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.nio.aAccept
import mu.KLogging
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import kotlin.coroutines.experimental.CoroutineContext

val DEFAULT_TCP_SERVER_PORT = 12345
val BACKLOG = 0
val CLIENT_READ_TIMEOUT = 30000L // 30 sec
val CLIENT_WRITE_TIMEOUT = 1000L // 1 sec
val BUFFER_SIZE = 1024

open class TcpServer(hostname: String? = null,
                     port: Int = DEFAULT_TCP_SERVER_PORT,
                     backlog: Int = BACKLOG,
                     val doOnNewSocketChannel: suspend (AsynchronousSocketChannel) -> Unit) {
    companion object : KLogging()

    lateinit var serverJob: Job

    val serverChannel = if (null != hostname) {
        AsynchronousServerSocketChannel
                .open()
                .bind(InetSocketAddress(hostname, port), backlog)
    } else {
        AsynchronousServerSocketChannel
                .open()
                .bind(InetSocketAddress(port), backlog)
    }

    fun launch(coroutineContext: CoroutineContext = DefaultDispatcher) {
        serverJob = launch(coroutineContext) {
            logger.info { "Listening on port $DEFAULT_TCP_SERVER_PORT" }
            // loop and accept connections forever
            while (true) {
                val client = serverChannel.aAccept()
                val address = try {
                    val ia = client.remoteAddress as InetSocketAddress
                    "${ia.address.hostAddress}:${ia.port}"
                } catch (ex: Throwable) {
                    logger.warn(ex) { "Accepted client connection but failed to get its address because of $ex" }
                    continue /* accept next connection */
                }
                logger.debug { "Accepted client connection from $address" }
                // just start a new coroutine for each client connection
                try {
                    doOnNewSocketChannel(client)
                    logger.debug { "Client connection from $address has terminated normally" }
                } catch (ex: Throwable) {
                    logger.warn(ex) { "Client connection from $address has terminated because of $ex" }
                }
            }
        }
    }

    fun shutdown() {
        logger.info { "Closing on port $DEFAULT_TCP_SERVER_PORT" }
        if (serverChannel.isOpen) {
            serverJob.cancel()
        }
    }
}