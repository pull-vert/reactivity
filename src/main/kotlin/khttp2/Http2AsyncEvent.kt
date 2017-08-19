package khttp2

import java.nio.channels.SelectableChannel

/**
 * Event handling interface from HttpClientImpl's selector.
 *
 * If BLOCKING is set, then the channel will be put in blocking
 * mode prior to handle() being called. If false, then it remains non-blocking.
 *
 * If REPEATING is set then the event is not cancelled after being posted.
 */
internal abstract class Http2AsyncEvent(protected val flags: Int) {

    /** Returns the channel  */
    abstract fun channel(): SelectableChannel

    /** Returns the selector interest op flags OR'd  */
    abstract fun interestOps(): Int

    /** Called when event occurs  */
    abstract fun handle()

    /** Called when selector is shutting down. Abort all exchanges.  */
    abstract fun abort()

    fun blocking(): Boolean {
        return flags and BLOCKING != 0
    }

    fun repeating(): Boolean {
        return flags and REPEATING != 0
    }

    companion object {

        val BLOCKING = 0x1 // non blocking if not set
        val REPEATING = 0x2 // one off event if not set
    }
}