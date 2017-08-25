package reactivity.experimental

import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

abstract class Schedulers private constructor() {

    companion object {
        @JvmStatic fun emptyCoroutineContext():Scheduler {
            return object: Scheduler {
                override val context: CoroutineContext
                    get() = EmptyCoroutineContext
            }
        }
    }
}

interface Scheduler {
    val context: CoroutineContext
}