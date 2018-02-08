package reactivity.experimental.coroutine

import kotlin.browser.window
import kotlin.coroutines.experimental.suspendCoroutine

actual suspend fun delay(time: Int): Unit = suspendCoroutine { cont ->
    window.setTimeout({ cont.resume(Unit) }, time)
}