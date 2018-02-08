package reactivity.http2.experimental

import java.util.concurrent.Executor
import java.util.concurrent.Flow
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters

interface KHttp2Server {

    companion object {
        /**
         * Returns a new KHttp2Server with default settings.
         *
         * @return a new KHttp2Server
         */
        fun newHttp2Server() = Http2ServerBuilderImpl().build()

        /**
         * Creates a new `HttpClient` coroutine.
         *
         * @return a `HttpClient.Builder`
         */
        fun newBuilder(): Builder {
            return Http2ServerBuilderImpl()
        }
    }

    fun <T> newHandler(handler: (Http2Request<T>, Http2Response) -> Flow.Publisher<Void>) : Flow.Publisher<Http2Context>

    interface Builder {

        /**
         * Returns a [KHttp2Server] built from the current state of this
         * coroutine.
         *
         * @return this coroutine
         */
        fun build(): KHttp2Server

        /**
         * The address to listen for (e.g. 0.0.0.0 or 127.0.0.1)
         *
         * @param bindAddress address to listen for (e.g. 0.0.0.0 or 127.0.0.1)
         * @return this coroutine
         */
        fun bindAddress(bindAddress: String): Builder

        /**
         * The port to listen to, or 0 to dynamically attribute one.
         *
         * @param port the port to listen to, or 0 to dynamically attribute one.
         * @return this coroutine
         */
        fun port(port: Int): Builder

        fun sslParameters(executor: Executor): Builder
        fun sslContext(sslContext: SSLContext): Builder
        fun sslParameters(sslParameters: SSLParameters): Builder
    }

    /**
     * Returns the {@code SSLContext}, if one was set on this server. If a security
     * manager is set, then the caller must have the
     * {@link java.net.NetPermission NetPermission}("getSSLContext") permission.
     * If no {@code SSLContext} was set, then the default context is returned.
     *
     * @return this server's SSLContext
     * @throws SecurityException if the caller does not have permission to get
     *         the SSLContext
     */
    fun sslContext(): SSLContext

    /**
     * Returns an {@code Optional} containing the {@link SSLParameters} set on
     * this server. If no {@code SSLParameters} were set in the client's coroutine,
     * then the {@code Optional} is empty.
     *
     * @return this server's {@code SSLParameters}
     */
    fun sslParameters(): SSLParameters

    /**
     * Returns the {@code Executor} set on this server. If an {@code
     * Executor} was not set on the client's coroutine, then a default
     * object is returned. The default {@code Executor} is created independently
     * for each server.
     *
     * @return this server's Executor
     */
    fun executor(): Executor
}
