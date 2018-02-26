package reactivity.experimental.http2

import kotlinx.coroutines.experimental.runBlocking
import reactivity.experimental.http2.ssl.createSSLEngine

object Http2ServerMain {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        Http2Server(sslEngine = createSSLEngine()).launch(coroutineContext)
    }
}