package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.selects.AbstractSelectBuilder
import kotlinx.coroutines.experimental.common.selects.AbstractUnbiasedSelectBuilder
import kotlinx.coroutines.experimental.common.selects.SelectBuilderCommon
import kotlin.coroutines.experimental.Continuation

expect interface SelectBuilder<in R> : SelectBuilderCommon<R>

expect class SelectBuilderImpl<in R>(delegate: Continuation<R>) : AbstractSelectBuilder<R>

expect class UnbiasedSelectBuilderImpl<in R>(cont: Continuation<R>) : AbstractUnbiasedSelectBuilder<R>