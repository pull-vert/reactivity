/**
 * Defines high-level HTTP2 Server API.
 *
 * @moduleGraph
 * @since 9
 */
module khttp2 {
    requires kotlin.stdlib;
    requires jdk.incubator.httpclient;
    exports io.khttp2;
}

