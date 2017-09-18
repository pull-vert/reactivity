package reactivity.core.experimental.internal.coroutines

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.selects.select
import kotlinx.coroutines.experimental.yield

/**
 * Indicates attempt to [receive][Consumer.await] on [isCompletedExceptionally][Consumer.isCompletedExceptionally]
 * channel that was closed _normally_. A _failed_ producer rethrows the original [close][Producer.close] cause
 * exception on receive attempts.
 */
internal class ClosedConsumerException(message: String?) : NoSuchElementException(message)

interface Producer<in E> {
    /**
     * Returns `true` if this consumer was closed by invocation of [close] and thus
     * the [produce] attempt throws [ClosedConsumerException]. If the consumer was closed because of the exception, it
     * is considered closed, too, but it is called a _failed_ consumer. Any suspending attempts to produce
     * the element to a failed consumer throw the original [close] cause exception.
     */
    val isClosedForProduce: Boolean

    /**
     * Adds [element] into this consumer, suspending the caller while this consumer awaits,
     * or throws [ClosedConsumerException] if the consumer [isClosedForProduce] _normally_.
     * It throws the original [close] cause exception if the consumer has _failed_.
     *
     * Note, that closing a consumer _after_ this function had suspended does not cause this suspended produce invocation
     * to abort, because closing a consumer is conceptually like producing a special "close token" over this consumer.
     * The element that is sent to the consumer is delivered in one to one. The element that
     * is being sent will get delivered to receiver before a close token.
     *
     * This suspending function is cancellable. If the [Job] of the current coroutine is cancelled or completed while this
     * function is suspended, this function immediately resumes with [CancellationException].
     *
     * *Cancellation of suspended produce is atomic* -- when this function
     * throws [CancellationException] it means that the [element] was not sent to this consumer.
     * As a side-effect of atomic cancellation, a thread-bound coroutine (to some UI thread, for example) may
     * continue to execute even after it was cancelled from the same thread in the case when this produce operation
     * was already resumed and the continuation was posted for execution to the thread's queue.
     *
     * Note, that this function does not check for cancellation when it is not suspended.
     * Use [yield] or [CoroutineScope.isActive] to periodically check for cancellation in tight loops if needed.
     *
     * This function cannot be used in [select] invocation for now
     * Use [complete] to try sending to this consumer without waiting.
     */
    suspend fun produce(element: E)

    /**
     * Adds [element] into this consumer if it is possible to do so immediately without violating restrictions
     * and returns `true`. Otherwise, it returns `false` immediately
     * or throws [ClosedConsumerException] if the consumer [isClosedForProduce] _normally_.
     * It throws the original [close] cause exception if the consumer has _failed_.
     */
    fun complete(element: E): Boolean

//    /**
//     * Registers [onSend][SelectBuilder.onSend] select clause.
//     * @suppress **This is unstable API and it is subject to change.**
//     */
//    public fun <R> registerSelectSend(select: SelectInstance<R>, element: E, block: suspend () -> R)

    /**
     * Closes this consumer with an optional exceptional [cause].
     * This is an idempotent operation -- repeated invocations of this function have no effect and return `false`.
     * Conceptually, its produce a special "close token" over this consumer. Immediately after invocation of this function
     * [isClosedForProduce] starts returning `true`.
     *
     * A consumer that was closed without a [cause], is considered to be _closed normally_.
     * A consumer that was closed with non-null [cause] is called a _failed consumer_. Attempts to produce or
     * await on a failed consumer throw this cause exception.
     */
    fun close(cause: Throwable? = null): Boolean
}