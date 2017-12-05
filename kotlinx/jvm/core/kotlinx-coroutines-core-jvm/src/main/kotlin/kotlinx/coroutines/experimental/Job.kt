package kotlinx.coroutines.experimental

import kotlinx.coroutines.experimental.common.DisposableHandle
import kotlinx.coroutines.experimental.common.Job
import kotlinx.coroutines.experimental.common.JobNode
import java.util.concurrent.Future

/**
 * Cancels a specified [future] when this job is complete.
 *
 * This is a shortcut for the following code with slightly more efficient implementation (one fewer object created).
 * ```
 * invokeOnCompletion { future.cancel(false) }
 * ```
 */
public fun Job.cancelFutureOnCompletion(future: Future<*>): DisposableHandle =
    invokeOnCompletion(handler = CancelFutureOnCompletion(this, future))

private class CancelFutureOnCompletion(
        job: Job,
        private val future: Future<*>
) : JobNode<Job>(job)  {
    override fun invoke(reason: Throwable?) {
        // Don't interrupt when cancelling future on completion, because no one is going to reset this
        // interruption flag and it will cause spurious failures elsewhere
        future.cancel(false)
    }
    override fun toString() = "CancelFutureOnCompletion[$future]"
}