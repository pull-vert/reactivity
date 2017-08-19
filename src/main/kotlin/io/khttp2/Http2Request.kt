package io.khttp2

import jdk.incubator.http.HttpHeaders
import java.net.URI
import javax.net.ssl.SSLParameters

/**
 *  Represents one HTTP request which was sent to the server.
 *
 * <p>A [Http2Request] is available when the request headers and body have been received,
 * and sometimes after the response body has also been received (if the
 * request contains a body). This depends on the request body handler provided when
 * handling the request. In all cases, the request body handler is invoked
 * before the body is read. This gives applications an opportunity to decide
 * how to handle the body.
 *
 * <p> Methods are provided in this class for accessing the request headers,
 * and request body.
 * <p>
 * <b>request handlers and processors</b>
 * <p>
 * Request bodies are handled at two levels. Application code supplies a request
 * handler ([BodyHandler]) which may examine the request headers,
 * and which then returns a [BodyProcessor] to actually read
 * (or discard) the body and convert it into some useful Java object type. The handler
 * can return one of the pre-defined processor types, or a custom processor, or
 * if the body is to be discarded, it can call [BodyProcessor.discard]
 * and return a processor which discards the response body.
 * Static implementations of both handlers and processors are provided in
 * [BodyHandler] and [BodyProcessor] respectively.
 * In all cases, the handler functions provided are convenience implementations
 * which ignore the supplied status code and
 * headers and return the relevant pre-defined [BodyProcessor].
 * <p>
 * See [BodyHandler] for example usage.
 *
 * @param T the request body type
 * @since 9
 */
abstract class Http2Request<T> {

    /**
     * Returns the received request headers.
     *
     * @return the request headers
     */
    abstract fun headers(): HttpHeaders

    /**
     * Returns the body. Depending on the type of `T`, the returned body may
     * represent the body after it was read (such as `byte[]`, or
     * `String`, or `Path`) or it may represent an object with
     * which the body is read, such as an [java.io.InputStream].
     *
     * @return the body
     */
    abstract fun body(): T

    /**
     * Returns the [javax.net.ssl.SSLParameters] in effect for this
     * request.
     *
     * @return the SSLParameters associated with the request
     */
    abstract fun sslParameters(): SSLParameters

    /**
     * Returns the `URI` that the request was received from.
     *
     * @return the URI of the response
     */
    abstract fun uri(): URI
}