package kotlinx.coroutines.experimental.common.expect

import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext

/**
 * This is the default [CoroutineContext] that is used by all standard builders like
 * [kotlinx.coroutines.experimental.launch], [kotlinx.coroutines.experimental.async], etc if no dispatcher nor any other [ContinuationInterceptor] is specified in their context.
 *
 * Depends on platform used
 */
expect val DefaultDispatcher: CoroutineContext

expect val Unconfined: CoroutineContext