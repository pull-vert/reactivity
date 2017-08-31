package reactivity.experimental

import kotlin.coroutines.experimental.EmptyCoroutineContext

/**
 * Exposes the provided [CoroutineContext] to static methods for Java
 */
object CoroutineContexts {
    @JvmStatic fun emptyCoroutineContext() = EmptyCoroutineContext
}