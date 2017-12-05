package reactivity.experimental.expect

import reactivity.experimental.IMulti
import reactivity.experimental.IMultiImpl

expect abstract class Multi<T>() : IMulti<T>

expect abstract class AMulti<T>() : Multi<T>, IMultiImpl<T>

expect fun <T> Publisher<T>.toMulti(): Multi<T>

expect fun <T> Publisher<T>.toMulti(scheduler: Scheduler): Multi<T>