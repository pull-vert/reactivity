/**
 * Defines high-level HTTP2 Server API.
 *
 * @moduleGraph
 * @since 9
 */
module khttp2 {
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires jdk.incubator.httpclient;
    exports io.khttp2;
    exports io.khttp2.internal.common;
}

