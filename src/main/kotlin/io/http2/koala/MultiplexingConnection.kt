package io.http2.koala

import io.http2.koala.internal.frame.Frame
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Flow

internal class MultiplexingConnection(server: KHttp2ServerImpl) : Closeable {

    @Volatile
    var closed: Boolean = false
    private val streams = ConcurrentHashMap<Int, Stream>()
    private var nextPushStream = 2

    override fun close() {
        closed = true
    }

    // Subscriber of input Request frames, Publisher to Stream input
    private val input = object : Flow.Processor<Frame, Frame> {
        /** calls onSubscribe of stream.input [onNext] */
        override fun subscribe(subscriber: Flow.Subscriber<in Frame>?) {
            val subscription = Stream.InputSubscription()
            subscriber!!.onSubscribe(subscription)
            // the stream.input.onComplete will be called when necessary !
            subscription.request(Long.MAX_VALUE)
        }

        /**
         * A new Frame arrived
         */
        override fun onNext(item: Frame?) {
            val streamId = item!!.streamId
            val stream = streams.computeIfAbsent(streamId) { streamId ->

                val stream: Stream = Stream(streamId, this@MultiplexingConnection)
                // I am the producer of the input of the Stream
                subscribe(stream.input)
                stream
            }
            stream.input.onNext(item)
            if (item.lastFrameOfMessage) stream.input.onComplete()
        }

        /**
         * normally mus never be called
         */
        override fun onComplete() {
            // a new Frame can occur until connection ends
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        /**
         * starts receiving the frames
         */
        override fun onSubscribe(subscription: Flow.Subscription?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onError(throwable: Throwable?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    // Subscriber of output Response frames
    internal val output = object : Flow.Subscriber<Frame> {

        /**
         * A new Frame arrived
         */
        override fun onNext(item: Frame?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        /**
         * normally mus never be called
         */
        override fun onComplete() {
            // a new Frame can occur until connection ends
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        /**
         * starts emmiting the frames
         */
        override fun onSubscribe(subscription: Flow.Subscription?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onError(throwable: Throwable?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}