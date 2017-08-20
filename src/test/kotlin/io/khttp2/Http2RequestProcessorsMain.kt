package io.khttp2

import coroutines.Swing
import coroutines.future
import java.nio.ByteBuffer

object Http2RequestProcessorsMain {
    @JvmStatic fun main(vararg args: String) {
        val proc = Http2RequestProcessors.ByteArrayProcessor { bytes: ByteArray -> String(bytes) }
        future(Swing) {
            proc.onNext(ByteBuffer.allocate(12))
//            delay(10, TimeUnit.SECONDS)
            proc.suspendingOnComplete()
            println("suspendingOnComplete")
            val s = proc.getBody().get()
            println("value " + s)
        }
    }
}