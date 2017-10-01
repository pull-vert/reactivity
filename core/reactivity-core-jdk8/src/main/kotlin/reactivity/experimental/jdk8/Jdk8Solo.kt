package reactivity.experimental.jdk8

import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import kotlinx.coroutines.experimental.reactive.awaitSingle
import reactivity.experimental.DEFAULT_SCHEDULER
import reactivity.experimental.Scheduler
import reactivity.experimental.Solo
import reactivity.experimental.solo
import java.util.concurrent.CompletableFuture

object Jdk8Solo {
    @JvmStatic
    fun <T> toCompletableFuture(solo: Solo<T>) = future(solo.initialScheduler.context) {
        solo.awaitSingle()
    }

    @JvmStatic
    fun <T> toCompletableFuture(scheduler: Scheduler, solo: Solo<T>) = future(scheduler.context) {
        solo.awaitSingle()
    }
}

object Jdk8SoloBuilder {
    /**
     * Creates a [Solo] from a [completableFuture]
     *
     * @return the [Solo]<T> created
     *
     * @param T the type of the input [completableFuture]
     */
    @JvmStatic
    fun <T> fromCompletableFuture(completableFuture: CompletableFuture<T>) = fromCompletableFuture(DEFAULT_SCHEDULER, completableFuture)

    /**
     * Creates a [Solo] from a [completableFuture]
     *
     * @return the [Solo]<T> created
     *
     * @param T the type of the input [completableFuture]
     */
    @JvmStatic
    fun <T> fromCompletableFuture(scheduler: Scheduler, completableFuture: CompletableFuture<T>) = solo(scheduler) {
        send(completableFuture.await())
    }
}