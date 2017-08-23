package io.http2.koala

import io.http2.koala.internal.common.Log
import io.http2.koala.internal.common.Utils
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.nio.channels.ClosedChannelException
import java.nio.channels.SelectableChannel
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Flow
import java.util.stream.Stream
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters

internal class KHttp2ServerImpl(builder: Http2ServerBuilderImpl) : KHttp2Server {

    private val executor: Executor
    // Security parameters
    private val sslContext: SSLContext
    private val sslParams: SSLParameters
    private val selmgr: SelectorManager

    /** A Set of, deadline first, ordered timeout events.  */
    private val timeouts: TreeSet<Http2TimeoutEvent>

    init {
        sslContext = builder.sslContext
        executor = builder.executor
        sslParams = builder.sslParams
        timeouts = TreeSet()
        try {
            selmgr = SelectorManager(this)
        } catch (e: IOException) {
            // unlikely
            throw InternalError(e)
        }

        selmgr.isDaemon = true
    }

    companion object {
        fun create(builder : Http2ServerBuilderImpl) : KHttp2ServerImpl {
            val impl = KHttp2ServerImpl(builder)
            impl.start()
            return impl
        }
    }

    private fun start() {
        selmgr.start()
        Log.logTrace("KHttp2ServerImpl started")
    }

    /**
     * Wait for activity on given exchange (assuming blocking = false).
     * It's a no-op if blocking = true. In particular, the following occurs
     * in the SelectorManager thread.
     *
     * 1) mark the connection non-blocking
     * 2) add to selector
     * 3) If selector fires for this exchange then
     * 4)   - mark connection as blocking
     * 5)   - call AsyncEvent.handle()
     *
     * If exchange needs to block again, then call registerEvent() again
     */
    @Throws(IOException::class)
    fun registerEvent(exchange: Http2AsyncEvent) {
        selmgr.register(exchange)
    }

    /**
     * Only used from RawChannel to disconnect the channel from
     * the selector
     */
    fun cancelRegistration(s: SocketChannel) {
        selmgr.cancel(s)
    }

    override fun <T> newHandler(handler: (Http2Request<T>, Http2Response) -> Flow.Publisher<Void>): Flow.Publisher<Http2Context> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

//    /**
//     * Create a [ContextHandler] for [ServerBootstrap.childHandler]
//     *
//     * @param handler user provided in/out handler
//     * @param sink user provided bind handler
//     *
//     * @return a new [ContextHandler]
//     */
//    protected fun doHandler(
//            handler: BiFunction<in NettyInbound, in NettyOutbound, out Flow.Publisher<Void>>,
//            sink: MonoSink<Http2Context>): ContextHandler<Channel> {
//        return ContextHandler.newServerContext(sink,
//                options,
//                loggingHandler(),
//                { ch, c, msg -> ChannelOperations.bind(ch, handler, c) })
//    }

    // Main loop for this server's selector
    // TODO : avoit inherit Thread and use Coroutines
    private class SelectorManager internal constructor(ref : KHttp2ServerImpl) : Thread(null, null, "SelectorManager", 0, false) {

        private val NODEADLINE = 3000L
        private val selector: Selector
        @Volatile private var closed: Boolean = false
        private val readyList: MutableList<Http2AsyncEvent>
        private val registrations: MutableList<Http2AsyncEvent>

        // Uses a weak reference to the KHttp2Server owning this
        // selector: a strong reference prevents its garbage
        // collection while the thread is running.
        // We want the thread to exit gracefully when the
        // KHttp2Server that owns it gets GC'ed.
        internal var ownerRef: WeakReference<KHttp2ServerImpl>

        init {
            ownerRef = WeakReference(ref)
            readyList = mutableListOf()
            registrations = mutableListOf()
            selector = Selector.open()
        }

        // This returns immediately. So caller not allowed to send/receive
        // on connection.

        @Synchronized
        @Throws(IOException::class)
        internal fun register(e: Http2AsyncEvent) {
            registrations.add(e)
            selector.wakeup()
        }

        @Synchronized
        internal fun cancel(e: SocketChannel) {
            val key = e.keyFor(selector)
            key?.cancel()
            selector.wakeup()
        }

        internal fun wakeupSelector() {
            selector.wakeup()
        }

        @Synchronized
        internal fun shutdown() {
            closed = true
            try {
                selector.close()
            } catch (ignored: IOException) {
            }
        }

        override fun run() {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    var server: KHttp2ServerImpl?
                    synchronized(this) {
                        for (exchange in registrations) {
                            val c = exchange.channel()
                            try {
                                c.configureBlocking(false)
                                val key = c.keyFor(selector)
                                val sa: SelectorAttachment
                                if (key == null || !key.isValid) {
                                    if (key != null) {
                                        // key is canceled.
                                        // invoke selectNow() to purge it
                                        // before registering the new event.
                                        selector.selectNow()
                                    }
                                    sa = SelectorAttachment(c, selector)
                                } else {
                                    sa = key.attachment() as SelectorAttachment
                                }
                                sa.register(exchange)
                            } catch (e: IOException) {
                                Log.logError("KHttp2ServerImpl: " + e)
                                c.close()
                                // let the exchange deal with it
                                handleEvent(exchange)
                            }

                        }
                        registrations.clear()
                    }

                    // Check whether server is still alive, and if not,
                    // gracefully stop this thread
                    server = ownerRef.get()
                    if (server == null) {
                        Log.logTrace("HttpClient no longer referenced. Exiting...")
                        return
                    }
                    val millis = server.purgeTimeoutsAndReturnNextDeadline()
                    server = null // don't hold onto the server ref

                    debugPrint(selector)
                    // Don't wait for ever as it might prevent the thread to
                    // stop gracefully. millis will be 0 if no deadline was found.
                    val n = selector.select(if (millis == 0L) NODEADLINE else millis)
                    if (n == 0) {
                        // Check whether server is still alive, and if not,
                        // gracefully stop this thread
                        server = ownerRef.get()
                        if (server == null) {
                            Log.logTrace("HttpClient no longer referenced. Exiting...")
                            return
                        }
                        server.purgeTimeoutsAndReturnNextDeadline()
                        server = null // don't hold onto the server ref
                        continue
                    }
                    val keys = selector.selectedKeys()

                    for (key in keys) {
                        val sa = key.attachment() as SelectorAttachment
                        val eventsOccurred = key.readyOps()
                        sa.events(eventsOccurred).forEach({ readyList.add(it) })
                        sa.resetInterestOps(eventsOccurred)
                    }
                    selector.selectNow() // complete cancellation
                    selector.selectedKeys().clear()

                    for (exchange in readyList) {
                        if (exchange.blocking()) {
                            exchange.channel().configureBlocking(true)
                        }
                        handleEvent(exchange) // will be delegated to executor
                    }
                    readyList.clear()
                }
            } catch (e: Throwable) {
                if (!closed) {
                    // This terminates thread. So, better just print stack trace
                    val err = Utils.stackTrace(e)
                    Log.logError("KHttp2ServerImpl: fatal error: " + err)
                }
            } finally {
                shutdown()
            }
        }

        internal fun debugPrint(selector: Selector) {
            System.err.println("Selector: debugprint start")
            val keys = selector.keys()
            for (key in keys) {
                val c = key.channel()
                val ops = key.interestOps()
                System.err.printf("selector chan:%s ops:%d\n", c, ops)
            }
            System.err.println("Selector: debugprint end")
        }

        internal fun handleEvent(e: Http2AsyncEvent) {
            if (closed) {
                e.abort()
            } else {
                e.handle()
            }
        }
    }

    /**
     * Tracks multiple user level registrations associated with one NIO
     * registration (SelectionKey). In this implementation, registrations
     * are one-off and when an event is posted the registration is cancelled
     * until explicitly registered again.
     *
     *
     *  No external synchronization required as this class is only used
     * by the SelectorManager thread. One of these objects required per
     * connection.
     */
    private class SelectorAttachment internal constructor(private val chan: SelectableChannel, private val selector: Selector) {
        private val pending: MutableList<Http2AsyncEvent>
        private var interestOps: Int = 0

        init {
            this.pending = mutableListOf()
        }

        @Throws(ClosedChannelException::class)
        internal fun register(e: Http2AsyncEvent) {
            val newOps = e.interestOps()
            val reRegister = interestOps and newOps != newOps
            interestOps = interestOps or newOps
            pending.add(e)
            if (reRegister) {
                // first time registration happens here also
                chan.register(selector, interestOps, this)
            }
        }

        /**
         * Returns a Stream<AsyncEvents> containing only events that are
         * registered with the given `interestOps`.
        </AsyncEvents> */
        internal fun events(interestOps: Int): Stream<Http2AsyncEvent> {
            return pending.stream()
                    .filter { ev -> ev.interestOps() and interestOps != 0 }
        }

        /**
         * Removes any events with the given `interestOps`, and if no
         * events remaining, cancels the associated SelectionKey.
         */
        internal fun resetInterestOps(interestOps: Int) {
            var newOps = 0

            val itr = pending.iterator()
            while (itr.hasNext()) {
                val event = itr.next()
                val evops = event.interestOps()
                if (event.repeating()) {
                    newOps = newOps or evops
                    continue
                }
                if (evops and interestOps != 0) {
                    itr.remove()
                } else {
                    newOps = newOps or evops
                }
            }

            this.interestOps = newOps
            val key = chan.keyFor(selector)
            if (newOps == 0) {
                key.cancel()
            } else {
                key.interestOps(newOps)
            }
        }
    }

    override fun sslContext(): SSLContext {
        Utils.checkNetPermission("getSSLContext")
        return sslContext
    }

    override fun sslParameters(): SSLParameters {
        return sslParams
    }

    override fun executor(): Executor {
        return executor
    }

    // Timer controls.
    // Timers are implemented through timed Selector.select() calls.

    @Synchronized
    fun registerTimer(event: Http2TimeoutEvent) {
        Log.logTrace("Registering timer {0}", event)
        timeouts.add(event)
        selmgr.wakeupSelector()
    }

    @Synchronized
    fun cancelTimer(event: Http2TimeoutEvent) {
        Log.logTrace("Canceling timer {0}", event)
        timeouts.remove(event)
    }

    /**
     * Purges ( handles ) timer events that have passed their deadline, and
     * returns the amount of time, in milliseconds, until the next earliest
     * event. A return value of 0 means that there are no events.
     */
    private fun purgeTimeoutsAndReturnNextDeadline(): Long {
        var diff = 0L
        var toHandle: MutableList<Http2TimeoutEvent>? = null
        var remaining = 0
        // enter critical section to retrieve the timeout event to handle
        synchronized(this) {
            if (timeouts.isEmpty()) return 0L

            val now = Instant.now()
            val itr = timeouts.iterator()
            while (itr.hasNext()) {
                val event = itr.next()
                diff = now.until(event.deadline(), ChronoUnit.MILLIS)
                if (diff <= 0) {
                    itr.remove()
                    toHandle = if (toHandle == null) ArrayList() else toHandle
                    toHandle!!.add(event)
                } else {
                    break
                }
            }
            remaining = timeouts.size
        }

        // can be useful for debugging
        if (toHandle != null && Log.trace()) {
            Log.logTrace("purgeTimeoutsAndReturnNextDeadline: handling "
                    + (if (toHandle == null) 0 else toHandle!!.size) + " events, "
                    + "remaining " + remaining
                    + ", next deadline: " + if (diff < 0) 0L else diff)
        }

        // handle timeout events out of critical section
        if (toHandle != null) {
            var failed: Throwable? = null
            for (event in toHandle!!) {
                try {
                    Log.logTrace("Firing timer {0}", event)
                    event.handle()
                } catch (e: Error) {
                    // Not expected. Handle remaining events then throw...
                    // If e is an OOME or SOE it might simply trigger a new
                    // error from here - but in this case there's not much we
                    // could do anyway. Just let it flow...
                    if (failed == null)
                        failed = e
                    else
                        (failed as java.lang.Throwable).addSuppressed(e)
                    Log.logTrace("Failed to handle event {0}: {1}", event, e)
                } catch (e: RuntimeException) {
                    if (failed == null)
                        failed = e
                    else
                        (failed as java.lang.Throwable).addSuppressed(e)
                    Log.logTrace("Failed to handle event {0}: {1}", event, e)
                }

            }
            if (failed is Error) throw failed
            if (failed is RuntimeException) throw failed
        }

        // return time to wait until next event. 0L if there's no more events.
        return if (diff < 0) 0L else diff
    }
}