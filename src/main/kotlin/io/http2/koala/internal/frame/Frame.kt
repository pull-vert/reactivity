package io.http2.koala.internal.frame

import java.util.concurrent.Flow

/**
 * @param lastFrameOfStream if this frame is the last of the stream
 */
abstract class Frame(val streamId: Int, val lastFrameOfMessage: Boolean) {
    // Subscriber of input Request frames
    private val input = object: Flow.Subscriber<Frame> {
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
    internal val output = object: Flow.Subscriber<Frame> {

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