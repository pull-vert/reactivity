package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.CommonPool
import kotlin.coroutines.experimental.CoroutineContext

actual val DefaultDispatcher: CoroutineContext = CommonPool

actual val Unconfined: CoroutineContext = kotlinx.coroutines.experimental.Unconfined