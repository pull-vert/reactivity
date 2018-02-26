package reactivity.experimental

import kotlinx.coroutines.experimental.Job
import kotlin.coroutines.experimental.CoroutineContext

@Suppress("EXPECTED_DECLARATION_WITH_DEFAULT_PARAMETER")
expect public fun newCoroutineContext(context: CoroutineContext, parent: Job? = null): CoroutineContext