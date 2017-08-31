/**
 * Defines high-level HTTP2 Server API.
 *
 * @moduleGraph
 */
module khttp2 {
    requires kotlin.stdlib;

    // reactivity requirements
    requires kotlinx.coroutines.core;
    requires kotlinx.coroutines.reactive;
//    requires atomicfu;
    requires reactive.streams;

    requires jdk.incubator.httpclient;
    exports io.http2.koala;
    exports reactivity.experimental;
//    exports io.khttp2.internal.common;
}

