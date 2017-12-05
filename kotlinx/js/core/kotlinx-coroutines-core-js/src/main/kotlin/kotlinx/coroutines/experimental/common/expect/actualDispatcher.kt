package kotlinx.coroutines.experimental.common.expect

import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

actual val DefaultDispatcher: CoroutineContext = EmptyCoroutineContext

actual val Unconfined: CoroutineContext = EmptyCoroutineContext