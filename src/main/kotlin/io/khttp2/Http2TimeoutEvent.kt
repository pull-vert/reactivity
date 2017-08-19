package io.khttp2

import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * Timeout event notified by selector thread. Executes the given handler if
 * the timer not canceled first.
 *
 * Register with [Http2ServerImpl.registerTimer].
 *
 * Cancel with [Http2ServerImpl.cancelTimer].
 */
internal abstract class Http2TimeoutEvent(duration: Duration) : Comparable<Http2TimeoutEvent> {
    // we use id in compareTo to make compareTo consistent with equals
    // see TimeoutEvent::compareTo below;
    private val id = COUNTER.incrementAndGet()
    private val deadline: Instant

    init {
        deadline = Instant.now().plus(duration)
    }

    abstract fun handle()

    fun deadline(): Instant {
        return deadline
    }

    override fun compareTo(other: Http2TimeoutEvent): Int {
        if (other === this) return 0
        // if two events have the same deadline, but are not equals, then the
        // smaller is the one that was created before (has the smaller id).
        // This is arbitrary and we don't really care which is smaller or
        // greater, but we need a total order, so two events with the
        // same deadline cannot compare == 0 if they are not equals.
        val compareDeadline = this.deadline.compareTo(other.deadline)
        if (compareDeadline == 0 && this != other) {
            val diff = this.id - other.id // should take care of wrap around
            if (diff < 0)
                return -1
            else if (diff > 0)
                return 1
            else
                assert(false) { "Different events with same id and deadline" }
        }
        return compareDeadline
    }

    override fun toString(): String {
        return "TimeoutEvent[id=$id, deadline=$deadline]"
    }

    companion object {

        private val COUNTER = AtomicLong()
    }
}
