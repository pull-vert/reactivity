package io.khttp2

import jdk.incubator.http.HttpRequest

/**
 * @param T the request body type
 * @param U the response body type
 * @since 9
 */
abstract class Http2Response<T, U> {
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
    abstract fun request(): Http2Request<T>
}