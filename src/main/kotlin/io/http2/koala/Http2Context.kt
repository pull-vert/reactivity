package io.http2.koala

import java.net.InetSocketAddress

/**
 * Hold contextual information for the underlying [Http2Pipe]
 *
 */
abstract class Http2Context {
    /**
     * Return remote address if remote channel [Http2Context] otherwise local
     * address if server selector channel.
     *
     * @return remote or local [InetSocketAddress]
     */
//    fun address(): InetSocketAddress {
//        val c = channel()
////        if (c is ServerSocketChannel) {
//            return c.serverAddress
////        }
////        throw IllegalStateException("Does not have an InetSocketAddress")
//    }

    /**
     * Return the underlying [Http2Connection]. Direct interaction might be considered
     * insecure if that affects the
     * underlying IO processing such as read, write or close or state such as pipeline
     * handler addition/removal.
     *
     * @return the underlying [Http2Pipe]
     */
    abstract internal fun channel(): MultiplexingConnection

    fun dispose() {
        channel().close()
    }
}