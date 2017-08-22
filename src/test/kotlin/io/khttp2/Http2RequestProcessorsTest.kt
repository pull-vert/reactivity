package io.khttp2

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.amshove.kluent.`should be greater than`
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.system.measureTimeMillis

class Http2RequestProcessorsTest {

    @Test
    fun `checks that CompletableDeferred from kotlinx works as expected`() = runBlocking {
//        val stub = mock(Http2RequestProcessors.ByteArrayProcessor::class)
//        When calling stub.toString() itReturns ""
        val delayMillis = 100L
        val proc = Http2RequestProcessors.ByteArrayProcessor { bytes: ByteArray -> String(bytes) }
        launch(newSingleThreadContext("")) {
            val time = measureTimeMillis {
                println("waiting for result")
                val s = proc.getBody().await()
                println("value " + s)
            }
            println("$time")
            time `should be greater than` delayMillis
        }
        proc.onNext(ByteBuffer.allocate(12))
        delay(delayMillis)
        proc.onComplete()
        println("onComplete")
    }
}