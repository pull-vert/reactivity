package reactivity.experimental.http2

import reactivity.experimental.Multi
import reactivity.experimental.Sink
import reactivity.experimental.tcp.BACKLOG
import reactivity.experimental.tcp.TcpServer
import java.nio.ByteBuffer
import javax.net.ssl.SSLEngine
import org.eclipse.jetty.alpn.ALPN





internal val DEFAULT_HTTP2_SERVER_PORT = 8080

class Http2Server(
        hostname: String? = null,
        port: Int = DEFAULT_HTTP2_SERVER_PORT,
        backlog: Int = BACKLOG,
        sslEngine: SSLEngine
) : TcpServer(hostname, port, backlog, { client ->

        object: Multi<ByteBuffer> {
                suspend override fun consume(sink: Sink<ByteBuffer>) {
                        var cause: Throwable? = null
                        try {
                                for (i in start until (start + count)) { sink.send(i) }
                        } catch (e: Throwable) {
                                cause = e
                        }
                        sink.close(cause)
                }
        }
}) {
        private val serverEngine: SSLEngine? = null     // server Engine
}