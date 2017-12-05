package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.selects.AbstractSelectBuilder
import kotlinx.coroutines.experimental.common.selects.AbstractUnbiasedSelectBuilder
import kotlinx.coroutines.experimental.common.selects.SelectBuilderCommon
import kotlin.coroutines.experimental.Continuation

actual interface SelectBuilder<in R> : SelectBuilderCommon<R>

actual class SelectBuilderImpl<in R> actual constructor(private val delegate: Continuation<R>)
    : AbstractSelectBuilder<R>(delegate), SelectBuilder<R>

actual class UnbiasedSelectBuilderImpl<in R> actual constructor(private val cont: Continuation<R>)
    : AbstractUnbiasedSelectBuilder<R>(cont), SelectBuilder<R>