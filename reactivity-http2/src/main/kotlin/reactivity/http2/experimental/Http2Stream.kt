package reactivity.http2.experimental

import reactivity.http2.experimental.internal.frame.Frame
import java.util.concurrent.Flow

/**
 * Http2Stream of input and output frames related to the same streamId
 */
internal class Http2Stream(streamId: Int, multiplexingConnection: MultiplexingConnection) {

    // Subscriber of input Request frames
    internal val input = object : Flow.Subscriber<Frame> {
        /**
         * A new Frame arrived
         */
        override fun onNext(item: Frame?) {

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
    private val output = object : Flow.Subscriber<Frame> {

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

    internal class InputSubscription : Flow.Subscription {
        override fun cancel() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun request(n: Long) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}