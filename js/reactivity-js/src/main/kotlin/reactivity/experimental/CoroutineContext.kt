package reactivity.experimental

import kotlinx.coroutines.experimental.Job
import kotlin.coroutines.experimental.CoroutineContext

actual public fun newCoroutineContext(context: CoroutineContext, parent: Job? = null): CoroutineContext
        = kotlinx.coroutines.experimental.newCoroutineContext(context, parent)