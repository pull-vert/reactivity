package io.khttp2

import jdk.incubator.http.HttpRequest
import jdk.incubator.http.HttpResponse

/**
 * Encapsulates multiple Exchanges belonging to one HttpRequestImpl.
 * - manages filters
 * - retries due to filters.
 * - I/O errors and most other exceptions get returned directly to user
 *
 * Creates a new Exchange for each request/response interaction
 */
internal class Http2ServerMultiExchange<U, T>(/*requestHandler: HttpResponse.BodyHandler<T>, userResponse: HttpRequest,
                                              server: Http2ServerImpl*/) {
}