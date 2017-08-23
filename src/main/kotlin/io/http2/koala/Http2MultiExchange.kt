package io.http2.koala

import reactor.core.publisher.Mono

/**
 * Encapsulates multiple Exchanges belonging to one HttpRequest
 * - manages filters
 * - retries due to filters.
 * - I/O errors and most other exceptions get returned directly to user
 *
 * Creates a new Exchange for each request/response interaction
 */
internal class Http2MultiExchange<U>(
        handler: (Http2Request<U>, Http2Response) -> Mono<Void>, http2ServerImpl: KHttp2ServerImpl) {
}