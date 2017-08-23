package io.http2.koala.internal.common

import jdk.incubator.http.HttpHeaders
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.UnsupportedEncodingException
import java.net.NetPermission
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

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

        /**
         * Get the Charset from the Content-encoding header. Defaults to
         * UTF_8
         */
        fun charsetFrom(headers: HttpHeaders): Charset {
            val encoding = headers.firstValue("Content-encoding")
                    .orElse("UTF_8")
            try {
                return Charset.forName(encoding)
            } catch (e: IllegalArgumentException) {
                return StandardCharsets.UTF_8
            }

        }
    }
}