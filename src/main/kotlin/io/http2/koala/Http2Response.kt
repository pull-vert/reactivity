package io.http2.koala

/**
 * @since 9
 */
abstract class Http2Response {
    /**
     * Returns the status code for this response.
     *
     * @return the response code
     */
    abstract fun statusCode(): Int

    /**
     * Returns the initial [Http2Request] that initiated the exchange.
     *
     * @return the request
     */
    abstract fun <T> request(): Http2Request<T>
}