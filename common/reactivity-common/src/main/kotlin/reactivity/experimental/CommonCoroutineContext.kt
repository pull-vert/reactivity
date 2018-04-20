package reactivity.experimental

import kotlinx.coroutines.experimental.Job
import kotlin.coroutines.experimental.CoroutineContext

expect fun newCoroutineContext(context: CoroutineContext, parent: Job? = null): CoroutineContext