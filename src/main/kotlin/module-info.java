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
    requires reactive.streams;
    exports io.http2.koala;
//    exports io.khttp2.internal.common;
}

