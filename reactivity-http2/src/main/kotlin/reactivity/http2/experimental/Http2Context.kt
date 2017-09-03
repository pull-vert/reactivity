package reactivity.http2.experimental

import java.net.InetSocketAddress

/**
 * Hold contextual information for the underlying [Http2Pipe]
 *
 */
abstract class Http2Context {
    /**
     * Return remote address if remote connection [Http2Context] otherwise local
     * address if server selector connection.
     *
     * @return remote or local [InetSocketAddress]
     */
    fun address(): InetSocketAddress {
        return connection().serverAddress
    }

    /**
     * Return the underlying [Http2Connection]. Direct interaction might be considered
     * insecure if that affects the
     * underlying IO processing such as read, write or close or state such as pipeline
     * handler addition/removal.
     *
     * @return the underlying [Http2Pipe]
     */
    abstract internal fun connection(): MultiplexingConnection

    fun dispose() {
        connection().close()
    }
}