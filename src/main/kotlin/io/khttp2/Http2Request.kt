package io.khttp2

import io.khttp2.Http2Request.Http2BodyHandler
import jdk.incubator.http.HttpHeaders
import jdk.incubator.http.HttpResponse
import jdk.incubator.http.ResponseProcessors
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.concurrent.Flow
import java.util.function.Supplier
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
 * handler ([Http2BodyHandler]) which may examine the request headers,
 * and which then returns a [BodyProcessor] to actually read
 * (or discard) the body and convert it into some useful Java object type. The handler
 * can return one of the pre-defined processor types, or a custom processor, or
 * if the body is to be discarded, it can call [BodyProcessor.discard]
 * and return a processor which discards the response body.
 * Static implementations of both handlers and processors are provided in
 * [Http2BodyHandler] and [BodyProcessor] respectively.
 * In all cases, the handler functions provided are convenience implementations
 * which ignore the supplied status code and
 * headers and return the relevant pre-defined [BodyProcessor].
 * <p>
 * @see Http2BodyHandler for example usage.
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

    /**
     * A handler for request bodies.
     * <p>
     * This is a function that takes one parameter: the request headers,
     * and which returns a [BodyProcessor].
     * The function is always called just before the request body is read. Its
     * implementation may examine the headers and must decide,
     * whether to accept the request body or discard it, and if accepting it,
     * exactly how to handle it.
     * <p>
     * Some pre-defined implementations which do not utilize the headers
     * (meaning the body is always accepted) are defined:
     * <ul><li>[Http2BodyHandler.asByteArray]</li>
     * <li>{@link #asByteArrayConsumer(java.util.function.Consumer)
     * asByteArrayConsumer(Consumer)}</li>
     * <li>{@link #asFileDownload(java.nio.file.Path,OpenOption...)
     * asFileDownload(Path,OpenOption...)}</li>
     * <li>{@link #discard(Object) }</li>
     * <li>{@link #asString(java.nio.charset.Charset)
     * asString(Charset)}</li></ul>
     * <p>
     * These implementations return the equivalent [BodyProcessor]
     * Alternatively, the handler can be used to examine the headers
     * and return different body processors as appropriate.
     * <p>
     * <b>Examples of handler usage</b>
     * <p>
     * The first example uses one of the predefined handler functions which
     * ignore the response headers, and always process the request
     * body in the same way.
     * <pre>
     * todo provide the real server code for both examples
     * {@code
     *      HttpResponse<Path> resp = HttpRequest
     *              .create(URI.create("http://www.foo.com"))
     *              .GET()
     *              .response(BodyHandler.asFile(Paths.get("/tmp/f")));
     * }
     * </pre>
     * Note, that even though these pre-defined handlers ignore the headers,
     * this information is still accessible from the [Http2Request]
     * when it is received.
     * <p>
     * In the second example, the function returns a different processor depending
     * on the status code.
     * <pre>
     * {@code
     *      HttpResponse<Path> resp1 = HttpRequest
     *              .create(URI.create("http://www.foo.com"))
     *              .GET()
     *              .response(
     *                  (status, headers) -> status == 200
     *                      ? BodyProcessor.asFile(Paths.get("/tmp/f"))
     *                      : BodyProcessor.discard(Paths.get("/NULL")));
     * }
     * </pre>
     *
     * @param T the request body type.
     */
    @FunctionalInterface
    interface Http2BodyHandler<T> {
        /**
         * Returns a [BodyProcessor] considering the given response status
         * code and headers. This method is always called before the body is read
         * and its implementation can decide to keep the body and store it somewhere
         * or else discard it, by  returning the `BodyProcessor` returned
         * from [discard()][BodyProcessor.discard].
         *
         * @param statusCode the HTTP status code received
         * @param responseHeaders the response headers received
         * @return a response body handler
         */
        abstract fun apply(statusCode: Int, responseHeaders: HttpHeaders): Http2BodyProcessor<T>
    }

    /**
     * A processor for request bodies.
     * <p>
     * The object acts as a [Flow.Subscriber]&lt;[ByteBuffer]&gt; to
     * the HTTP server implementation which receives ByteBuffers containing the
     * request body. The processor converts the incoming buffers of data to
     * some user-defined object type {@code T}.
     * <p>
     * The [Http2BodyProcessor.getBody] method returns a [Supplier] `T`
     * that provides the response body object. The {@code CompletionStage} must
     * be obtainable at any time. When it completes depends on the nature
     * of type {@code T}. In many cases, when {@code T} represents the entire body after being
     * read then it completes after the body has been read. If {@code T} is a streaming
     * type such as {@link java.io.InputStream} then it completes before the
     * body has been read, because the calling code uses it to consume the data.
     *
     * @param T the response body type
     */
    interface Http2BodyProcessor<T> : Flow.Subscriber<ByteBuffer> {
        /**
         * Returns a `Supplier` which when completed will return the
         * response body object.
         *
         * @return a Supplier for the response body
         */
        fun getBody(): Supplier<T>

        /**
         * Returns a body processor which stores the response body as a `String` converted using the given `Charset`.
         *
         *
         * The [HttpResponse] using this processor is available after the
         * entire response has been read.
         *
         * @param charset the character set to convert the String with
         * @return a body processor
         */
        fun asString(charset: Charset): Http2BodyProcessor<String> {
            return ResponseProcessors.ByteArrayProcessor<T> { bytes -> String(bytes, charset) }
        }
    }
}