package reactivity.core.experimental

import kotlinx.coroutines.experimental.channels.ProducerJob
import kotlinx.coroutines.experimental.reactive.asPublisher
import kotlin.coroutines.experimental.CoroutineContext

class MultiGrouped<T, R> internal constructor(val producerJob: ProducerJob<T>, coroutineContext: CoroutineContext, val key: R) : MultiImpl<T>(producerJob.asPublisher(coroutineContext)) {
}