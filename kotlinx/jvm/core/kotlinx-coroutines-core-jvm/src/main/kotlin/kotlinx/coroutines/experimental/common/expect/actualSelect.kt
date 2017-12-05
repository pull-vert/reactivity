package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.common.intrinsics.startCoroutineUndispatched
import kotlinx.coroutines.experimental.common.selects.AbstractSelectBuilder
import kotlinx.coroutines.experimental.common.selects.AbstractUnbiasedSelectBuilder
import kotlinx.coroutines.experimental.common.selects.SelectBuilderCommon
import kotlinx.coroutines.experimental.common.selects.SelectClause1
import kotlinx.coroutines.experimental.common.startCoroutineCancellable
import kotlin.coroutines.experimental.Continuation

actual interface SelectBuilder<in R> : SelectBuilderCommon<R> {
    /**
     * Clause that selects the given [block] after a specified timeout passes.
     *
     * @param time timeout time
     * @param unit timeout unit (milliseconds by default)
     */
    public fun onTimeout(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS, block: suspend () -> R)
}

actual class SelectBuilderImpl<in R> actual constructor(private val delegate: Continuation<R>)
    : AbstractSelectBuilder<R>(delegate), SelectBuilder<R> {
    override fun onTimeout(time: Long, unit: TimeUnit, block: suspend () -> R) {
        require(time >= 0) { "Timeout time $time cannot be negative" }
        if (time == 0L) {
            if (trySelect(null))
                block.startCoroutineUndispatched(completion)
            return
        }
        val action = Runnable {
            // todo: we could have replaced startCoroutine with startCoroutineUndispatched
            // But we need a way to know that Delay.invokeOnTimeout had used the right thread
            if (trySelect(null))
                block.startCoroutineCancellable(completion) // shall be cancellable while waits for dispatch
        }
        disposeOnSelect(context.delay.invokeOnTimeout(time, unit, action))
    }
}

actual class UnbiasedSelectBuilderImpl<in R> actual constructor(private val cont: Continuation<R>)
    : AbstractUnbiasedSelectBuilder<R>(cont), SelectBuilder<R> {

    override fun onTimeout(time: Long, unit: TimeUnit, block: suspend () -> R) {
        clauses += { (instance as SelectBuilderImpl).onTimeout(time, unit, block) }
    }
}