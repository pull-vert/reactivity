/**
 * Defines high-level HTTP2 Server API.
 *
 * @moduleGraph
 */
module khttp2 {
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires jdk.incubator.httpclient;
    requires reactor.core;
    exports io.http2.koala;
//    exports io.khttp2.internal.common;
}

