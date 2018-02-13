package coroutines

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.RecursiveAction
import java.util.concurrent.atomic.AtomicLongFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor

fun ForkJoinPool(parallel: Int) = Pool(ForkJoinPool.commonPool(), parallel)

/**
 * "The" Interceptor Element for this Context
 */
class Pool(
        val pool: ForkJoinPool,
        parallel: Int
) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {

    private val RA_CONTINUATION_UPDATER = AtomicReferenceFieldUpdater.newUpdater(Pool::class.java,
            RecursiveActionContinuation::class.java, "raContinuation")
    @Volatile
    private var raContinuation: RecursiveActionContinuation<*>? = null
    private val INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater(Pool::class.java, "index")
    @Volatile
    private var index = 0L
    @JvmField
    protected val mask: Int = parallel - 1

    init {
        check(parallel > 0) { "capacity must be positive" }
        check(parallel and mask == 0) { "capacity must be a power of 2" }
    }

    protected fun calcElementOffset(index: Long) = index.toInt() and mask

    protected fun lvIndex() = index
    protected fun soLazyIndex(newValue: Long) {
        INDEX_UPDATER.lazySet(this, newValue)
    }

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        val index = lvIndex()
        val offset = calcElementOffset(index)
        soLazyIndex(index + 1)
        // create and return a new parent RecursiveActionContinuation
        if (0 == offset) {
            val newRAContinuation = RecursiveActionContinuation(index, continuation.context.fold(continuation, { cont, element ->
                if (element != this@Pool && element is ContinuationInterceptor)
                    element.interceptContinuation(cont) else cont
            }), forkJoinPool = pool)
            RA_CONTINUATION_UPDATER.set(this, newRAContinuation)
            return newRAContinuation
        }
        // otherwise create a new child RecursiveActionContinuation
        // todo handle already computed parent
        val raContinuation = this.raContinuation
        return RecursiveActionContinuation(index, continuation.context.fold(continuation, { cont, element ->
            if (element != this@Pool && element is ContinuationInterceptor)
                element.interceptContinuation(cont) else cont
        }), raContinuation)
    }
}

private class RecursiveActionContinuation<T>(
        val index: Long,
        val continuation: Continuation<T>,
        val parent: RecursiveActionContinuation<*>? = null,
        val forkJoinPool: ForkJoinPool? = null
) : RecursiveAction(), Continuation<T> by continuation {

    var result: Any? = null // T | Throwable

    init {
        // fork execution of parent at init
        parent?.fork()
    }

    override fun resume(value: T) {
        println("resume index=$index value=$value")
        result = value
        forkJoinPool?.execute(this) ?: compute()
    }

    override fun resumeWithException(exception: Throwable) {
        println("resumeWithException index=$index exception=$exception")
        result = exception
        forkJoinPool?.execute(this) ?: compute()
    }

    @Suppress("UNCHECKED_CAST")
    override fun compute() {
        // handle parent
        parent?.join()
        println("compute index=$index result=$result")
        if (result is Throwable) continuation.resumeWithException(result as Throwable)
        continuation.resume(result as T)
    }

}

