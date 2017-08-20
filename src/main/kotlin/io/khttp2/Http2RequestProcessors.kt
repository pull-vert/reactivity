package io.khttp2

import io.khttp2.internal.common.SuspendingSubscriber
import io.khttp2.internal.common.Utils
import io.khttp2.internal.common.buildSupplier
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Flow
import java.util.function.Supplier

internal class Http2RequestProcessors {
    internal abstract class AbstractProcessor<T> : Http2Request.Http2BodyProcessor<T> {
        @get:Synchronized
        @set:Synchronized
        lateinit var server: Http2ServerImpl
    }

    internal class ByteArrayProcessor<T>(val transform: (ByteArray) -> T) : AbstractProcessor<T>(), SuspendingSubscriber<ByteBuffer> {

        private val supplierBuilder = buildSupplier<T> {}
        private val received = ArrayList<ByteBuffer>()

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

        suspend override fun onErrorSuspend(throwable: Throwable) {
            received.clear()
            supplierBuilder.completeExceptionally(throwable)
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

        suspend override fun onCompleteSuspend() {
            try {
                supplierBuilder.complete(transform.invoke(join(received)))
                received.clear()
            } catch (e: IllegalArgumentException) {
                supplierBuilder.completeExceptionally(e)
            }
        }

        override fun getBody(): Supplier<T> {
            return supplierBuilder
        }
    }
}