package reactivity.experimental.http2

import reactivity.experimental.http2.ssl.performTLSHandshake
import reactivity.experimental.tcp.BACKLOG
import reactivity.experimental.tcp.TcpServer
import javax.net.ssl.SSLEngine


internal val DEFAULT_HTTP2_SERVER_PORT = 8080

class Http2Server(
        hostname: String? = null,
        port: Int = DEFAULT_HTTP2_SERVER_PORT,
        backlog: Int = BACKLOG,
        sslEngine: SSLEngine
) : TcpServer(hostname, port, backlog, { client ->
        // 1 first step : ALPN TLS handshake
        performTLSHandshake(sslEngine, client)


//        object: Multi<ByteBuffer> {
//                suspend override fun consume(sink: Sink<ByteBuffer>) {
//                        var cause: Throwable? = null
//                        try {
////                                sink.send("")
//                        } catch (e: Throwable) {
//                                cause = e
//                        }
//                        sink.close(cause)
//                }
//        }
}) {
}