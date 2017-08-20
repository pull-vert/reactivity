package io.khttp2.internal.common

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.UnsupportedEncodingException
import java.net.NetPermission
import java.nio.ByteBuffer

class Utils {

    companion object {
        //        internal fun getNetProperty(name: String): String? {
//            return AccessController.doPrivileged({ NetProperties.get(name) } as PrivilegedAction<String>)
//        }

        fun stackTrace(t: Throwable): String? {
            val bos = ByteArrayOutputStream()
            var s: String? = null
            try {
                val p = PrintStream(bos, true, "US-ASCII")
                (t as java.lang.Throwable).printStackTrace(p)
                s = bos.toString("US-ASCII")
            } catch (ex: UnsupportedEncodingException) {
                // can't happen
            }

            return s
        }

        fun checkNetPermission(target: String) {
            val sm = System.getSecurityManager() ?: return
            val np = NetPermission(target)
            sm.checkPermission(np)
        }

        fun remaining(bufs: List<ByteBuffer>): Int {
            var remain = 0
            for (buf in bufs) {
                remain += buf.remaining()
            }
            return remain
        }
    }
}