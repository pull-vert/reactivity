package reactivity.experimental

fun <T> Multi<Publisher<T>>.merge() = merge(initialScheduler)

fun <T> Solo<Publisher<T>>.merge() = merge(initialScheduler)

// TODO : Junit to test that !!
@Suppress("EXPECTED_DECLARATION_WITH_DEFAULT_PARAMETER")
expect fun <T> Publisher<Publisher<T>>.merge(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER): Multi<T>

// Solo
fun <T> T.toSolo(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = solo(scheduler) {
    send(this@toSolo)
}

// Multi
fun <T> Iterable<T>.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = multi(scheduler) {
    for (x in this@toMulti) send(x)
}
fun BooleanArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun ByteArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun CharArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun DoubleArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun FloatArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun IntArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun LongArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun ShortArray.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = this.toList().toMulti(scheduler)
fun <T> Array<T>.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = multi(scheduler) {
    for (x in this@toMulti) send(x)
}
fun <T> Sequence<T>.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER) = multi(scheduler) {
    for (x in this@toMulti) send(x)
}

@Suppress("EXPECTED_DECLARATION_WITH_DEFAULT_PARAMETER")
expect fun <T> Publisher<T>.toMulti(scheduler: Scheduler = SCHEDULER_DEFAULT_DISPATCHER, key: Multi.Key<*>? = null): Multi<T>
