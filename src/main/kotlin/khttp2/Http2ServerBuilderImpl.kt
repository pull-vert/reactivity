package khttp2

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters


internal data class Http2ServerBuilderImpl(
        var bindAddress: String = "127.0.0.1",
        var port: Int = 80,
        var sslContext: SSLContext = SSLContext.getDefault(),
        var sslParams: SSLParameters = getDefaultParams(sslContext),
        var executor: Executor = Executors.newCachedThreadPool(DefaultThreadFactory.INSTANCE)) : Http2Server.Builder {

    // Define the default factory as a static inner class
    // that embeds all the necessary logic to avoid
    // the risk of using a lambda that might keep a reference on the
    // HttpClient instance from which it was created (helps with
    // heapdump analysis).
    private class DefaultThreadFactory private constructor() : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val t = Thread(null, r, "Http2Server_worker", 0, true)
            t.isDaemon = true
            return t
        }

        companion object {
            internal val INSTANCE = DefaultThreadFactory()
        }
    }

    override fun bindAddress(bindAddress: String): Http2ServerBuilderImpl {
        this.bindAddress = bindAddress
        return this
    }

    override fun port(port: Int): Http2ServerBuilderImpl {
        this.port = port
        return this
    }

    override fun sslContext(sslContext: SSLContext): Http2ServerBuilderImpl {
        this.sslContext = sslContext
        return this
    }

    override fun sslParameters(sslParameters: SSLParameters): Http2ServerBuilderImpl {
        this.sslParams = sslParameters
        return this
    }

    override fun sslParameters(executor: Executor): Http2ServerBuilderImpl {
        this.executor = executor
        return this
    }

    override fun build(): Http2Server = Http2ServerImpl.create(this)
}

private fun getDefaultParams(ctx: SSLContext): SSLParameters {
    val params = ctx.supportedSSLParameters
    params.protocols = arrayOf("TLSv1.2")
    return params
}