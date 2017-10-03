package reactivity.http2.experimental

import reactivity.experimental.core.SoloPublisher

/**
 * Encapsulates multiple Exchanges belonging to one HttpRequest
 * - manages filters
 * - retries due to filters.
 * - I/O errors and most other exceptions get returned directly to user
 *
 * Creates a new Exchange for each request/response interaction
 */
internal class Http2MultiExchange<U>(
        handler: (Http2Request<U>, Http2Response) -> SoloPublisher<Void>, http2ServerImpl: KHttp2ServerImpl) {
}