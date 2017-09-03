package reactivity.http2.experimental

import reactivity.core.experimental.multi
import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

internal class MultiplexingConnection(context: CoroutineContext = EmptyCoroutineContext,
                                      server: KHttp2ServerImpl, val serverAddress: InetSocketAddress) : Closeable {

    @Volatile
    var closed: Boolean = false
//    private val streams = ConcurrentHashMap<Int, Http2Stream>()
//    private var nextPushStream = 2

    override fun close() {
        closed = true
    }

    init {
        multi(context) {
            send(ByteBuffer.allocate(12))
        }/*.reduceWith()*/
    }

    // Subscriber of input Request frames, Publisher to Http2Stream input
//    private val input = object : Flow.Processor<Frame, Frame> {
//        /** calls onSubscribe of stream.input [onNext] */
//        override fun subscribe(subscriber: Flow.Subscriber<in Frame>?) {
//            val subscription = Http2Stream.InputSubscription()
//            subscriber!!.onSubscribe(subscription)
//            // the stream.input.onComplete will be called when necessary !
//            subscription.request(Long.MAX_VALUE)
//        }
//
//        /**
//         * A new Frame arrived
//         */
//        override fun onNext(item: Frame?) {
//            val streamId = item!!.streamId
//            val stream = streams.computeIfAbsent(streamId) { streamId ->
//
//                val stream: Http2Stream = Http2Stream(streamId, this@MultiplexingConnection)
//                // I am the producer of the input of the Http2Stream
//                subscribe(stream.input)
//                stream
//            }
//            stream.input.onNext(item)
//            if (item.lastFrameOfMessage) stream.input.onComplete()
//        }
//
//        /**
//         * normally mus never be called
//         */
//        override fun onComplete() {
//            // a new Frame can occur until connection ends
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//
//        /**
//         * starts receiving the frames
//         */
//        override fun onSubscribe(subscription: Flow.Subscription?) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//
//        override fun onError(throwable: Throwable?) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//    }
//
//    // Subscriber of output Response frames
//    internal val output = object : Flow.Subscriber<Frame> {
//
//        /**
//         * A new Frame arrived
//         */
//        override fun onNext(item: Frame?) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//
//        /**
//         * normally mus never be called
//         */
//        override fun onComplete() {
//            // a new Frame can occur until connection ends
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//
//        /**
//         * starts emmiting the frames
//         */
//        override fun onSubscribe(subscription: Flow.Subscription?) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//
//        override fun onError(throwable: Throwable?) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//    }
}