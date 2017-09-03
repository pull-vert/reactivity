package reactivity.http2.experimental

import reactivity.http2.experimental.internal.common.Utils
import kotlinx.coroutines.experimental.CompletableDeferred
import java.nio.ByteBuffer
import java.util.concurrent.Flow

internal class Http2RequestProcessors {
    internal abstract class AbstractProcessor<T> : Http2Request.Http2BodyProcessor<T> {
        @get:Synchronized
        @set:Synchronized
        lateinit var server: KHttp2ServerImpl
    }

    internal class ByteArrayProcessor<T>(val finisher: (ByteArray) -> T) : AbstractProcessor<T>() {
        private val result = CompletableDeferred<T>()
        private val received = mutableListOf<ByteBuffer>()

        private var subscription: Flow.Subscription? = null

        override fun onSubscribe(subscription: Flow.Subscription) {
            if (this.subscription != null) {
                subscription.cancel()
                return
            }
            this.subscription = subscription
            // We can handle whatever you've got
            subscription.request(java.lang.Long.MAX_VALUE)
        }

        override fun onNext(item: ByteBuffer) {
            // incoming buffers are allocated by http server internally,
            // and won't be used anywhere except this place.
            // So it's free simply to store them for further processing.
            if (item.hasRemaining()) {
                received.add(item)
            }
        }

        override fun onError(throwable: Throwable) {
            received.clear()
            result.completeExceptionally(throwable)
        }

        private fun join(bytes: List<ByteBuffer>): ByteArray {
            val size = Utils.remaining(bytes)
            val res = ByteArray(size)
            var from = 0
            for (b in bytes) {
                val l = b.remaining()
                b.get(res, from, l)
                from += l
            }
            return res
        }

        override fun onComplete() {
            try {
                result.complete(finisher.invoke(join(received)))
                received.clear()
            } catch (e: IllegalArgumentException) {
                result.completeExceptionally(e)
            }
        }

        override fun getBody() = result
    }
}