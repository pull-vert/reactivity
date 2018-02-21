package reactivity.experimental.tcp

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.nio.aConnect
import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import mu.KotlinLogging
import org.junit.Assert
import org.junit.Test
import reactivity.experimental.TestBase
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

private val logger = KotlinLogging.logger {}

class TcpServerTest: TestBase() {
    @Test
    fun tcpServerReadAndWrite() = runTest {
        val server = TcpServer(doOnNewSocketChannel = { client ->
            val buffer = ByteBuffer.allocate(2)
            client.aRead(buffer)
            buffer.flip()
            val received = Charsets.UTF_8.decode(buffer).toString()
            logger.debug { "server received $received" }
            Assert.assertEquals("OK", received)

            client.aWrite(Charsets.UTF_8.encode("123"))
        })
        server.launch(coroutineContext)

        val c2 = launch(coroutineContext) {
            val connection =
                    AsynchronousSocketChannel.open()
            // async calls
            connection.aConnect(InetSocketAddress("127.0.0.1", DEFAULT_SERVER_PORT))
            connection.aWrite(Charsets.UTF_8.encode("OK"))
            logger.debug { "writing OK" }

            val buffer = ByteBuffer.allocate(3)

            // async call
            connection.aRead(buffer)
            buffer.flip()
            val received = Charsets.UTF_8.decode(buffer).toString()
            logger.debug { "client received $received" }
            Assert.assertEquals("123", received)
            server.shutdown()
        }

        server.serverJob.join()
        c2.join()
    }
}