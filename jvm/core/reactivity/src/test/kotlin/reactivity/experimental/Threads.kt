package reactivity.experimental

fun currentThreads(): Set<Thread> {
    var estimate = 0
    while (true) {
        estimate = estimate.coerceAtLeast(Thread.activeCount() + 1)
        val arrayOfThreads = Array<Thread?>(estimate) { null }
        val n = Thread.enumerate(arrayOfThreads)
        if (n >= estimate) {
            estimate = n + 1
            continue // retry with a better size estimate
        }
        val threads = hashSetOf<Thread>()
        for (i in 0 until n)
            threads.add(arrayOfThreads[i]!!)
        return threads
    }
}
