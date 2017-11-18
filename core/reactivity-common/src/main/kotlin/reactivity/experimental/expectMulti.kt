package reactivity.experimental

expect abstract class Multi<T> : IMulti<T>

expect abstract class AMulti<T> : Multi<T>, IMultiImpl<T>

expect fun <T> Publisher<T>.toMulti(): Multi<T>

expect fun <T> Publisher<T>.toMulti(scheduler: Scheduler): Multi<T>