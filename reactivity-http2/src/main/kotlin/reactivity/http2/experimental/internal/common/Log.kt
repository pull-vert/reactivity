package reactivity.http2.experimental.internal.common

import java.util.*

abstract class Log : System.Logger {

    companion object {
        internal val logProp = "khttp2.KHttp2Server.log"

        internal val default = "all,frames:all"

        val OFF = 0
        val ERRORS = 0x1
        val REQUESTS = 0x2
        val HEADERS = 0x4
        val CONTENT = 0x8
        val FRAMES = 0x10
        val SSL = 0x20
        val TRACE = 0x40
        internal var logging: Int = 0

        // Frame types: "control", "data", "window", "all"
        val CONTROL = 1 // all except DATA and WINDOW_UPDATES
        val DATA = 2
        val WINDOW_UPDATES = 4
        val ALL = CONTROL or DATA or WINDOW_UPDATES
        internal var frametypes: Int = 0

        internal val logger: System.Logger?

        init {
//            val s = Utils.getNetProperty(logProp)
            val s = default
            if (s == null) {
                logging = OFF
            } else {
                val vals = s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (`val` in vals) {
                    when (`val`.toLowerCase(Locale.US)) {
                        "errors" -> logging = logging or ERRORS
                        "requests" -> logging = logging or REQUESTS
                        "headers" -> logging = logging or HEADERS
                        "content" -> logging = logging or CONTENT
                        "ssl" -> logging = logging or SSL
                        "trace" -> logging = logging or TRACE
                        "all" -> logging = logging or (CONTENT or HEADERS or REQUESTS or FRAMES or ERRORS or TRACE or SSL)
                    }
                    if (`val`.startsWith("frames")) {
                        logging = logging or FRAMES
                        val types = `val`.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (types.size == 1) {
                            frametypes = CONTROL or DATA or WINDOW_UPDATES
                        } else {
                            for (type in types) {
                                when (type.toLowerCase()) {
                                    "control" -> frametypes = frametypes or CONTROL
                                    "data" -> frametypes = frametypes or DATA
                                    "window" -> frametypes = frametypes or WINDOW_UPDATES
                                    "all" -> frametypes = ALL
                                }
                            }
                        }
                    }
                }
            }
            if (logging != OFF) {
                logger = System.getLogger("khttp2.KHttp2Server")
            } else {
                logger = null
            }
        }

        fun logError(s: String, vararg s1: Any) {
            if (errors()) {
                logger?.log(System.Logger.Level.INFO, "ERROR: " + s, *s1)
            }
        }

        fun logTrace(s: String, vararg s1: Any) {
            if (trace()) {
                logger?.log(System.Logger.Level.INFO, "TRACE: " + s, *s1)
            }
        }

        fun errors(): Boolean {
            return logging and ERRORS != 0
        }

        fun trace(): Boolean {
            return logging and TRACE != 0
        }
    }
}