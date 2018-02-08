package reactivity.experimental.coroutine

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.startCoroutine

interface CoroutineScope {
    val coroutineContext: CoroutineContext
}

fun coroutine(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
) {
    val coroutine = SimpleCoroutine<Unit>(context)
    block.startCoroutine(coroutine, coroutine)
}

class SimpleCoroutine<in T>(
        override val context: CoroutineContext
): Continuation<T>, CoroutineScope { // CoroutineContext.Element,

//    override val key: CoroutineContext.Key<*>
//        get() = object : CoroutineContext.Key<SimpleCoroutine<*>> { }

    /**
     * Completes execution of this coroutine normally with the specified [value].
     */
    override fun resume(value: T) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Completes execution of this with coroutine exceptionally with the specified [exception].
     */
    override fun resumeWithException(exception: Throwable) {
        throw exception
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val coroutineContext: CoroutineContext
        get() = context
}