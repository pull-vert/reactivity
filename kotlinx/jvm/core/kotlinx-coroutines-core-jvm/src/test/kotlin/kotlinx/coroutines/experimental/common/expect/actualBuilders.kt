package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.CoroutineScope
import kotlin.coroutines.experimental.CoroutineContext

actual fun <T> runBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T = kotlinx.coroutines.experimental.runBlocking(context, block)