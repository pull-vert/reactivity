package reactivity.experimental.coroutine

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

private val executor = Executors.newSingleThreadScheduledExecutor {
    Thread(it, "scheduler").apply { isDaemon = true }
}

actual suspend fun delay(time: Int) = delay(time.toLong(), TimeUnit.MILLISECONDS)

suspend fun delay(time: Long, unit: TimeUnit): Unit = suspendCoroutine { cont ->
    executor.schedule({ cont.resume(Unit) }, time, unit)
}