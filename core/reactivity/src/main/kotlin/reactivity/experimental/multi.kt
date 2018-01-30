package reactivity.experimental

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import kotlinx.coroutines.experimental.launch
import reactivity.experimental.channel.SpScChannel
import kotlin.coroutines.experimental.CoroutineContext

private const val DEFAULT_CLOSE_MESSAGE = "SpScChannel was closed"

// -------------- Intermediate (transforming) operations

fun <E : Any> Multi<E>.async(context: CoroutineContext = DefaultDispatcher, buffer: Int = 0): Multi<E> {
    val channel = SpScChannel<E>(buffer)
    return object : Multi<E> {
        suspend override fun consume(sink: Sink<E>) {
            launch(context) {
                try {
                    while (true) {
                        sink.send(channel.receive())
                    }
                } catch (e: Throwable) {
                    if (e is ClosedReceiveChannelException) sink.close(null)
                    else sink.close(e)
                }
            }

            var cause: Throwable? = null
            try {
                this@async.consume(object : Sink<E> {
                    suspend override fun send(item: E) {
                        channel.send(Element(item))
                    }

                    override fun close(cause: Throwable?) {
                        cause?.let { throw it }
                    }
                })
            } catch (e: Throwable) {
                cause = e
            }
            val closeCause = cause ?: ClosedReceiveChannelException(DEFAULT_CLOSE_MESSAGE)
            channel.send(Element(closeCause = closeCause))
        }
    }
}
