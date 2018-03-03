package reactivity.experimental

import kotlinx.coroutines.experimental.Job
import kotlin.coroutines.experimental.CoroutineContext

actual fun newCoroutineContext(context: CoroutineContext, parent: Job? = null): CoroutineContext
        = kotlinx.coroutines.experimental.newCoroutineContext(context, parent)