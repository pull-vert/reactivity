package reactivity.experimental.jdk8

import org.reactivestreams.Publisher
import reactivity.experimental.Scheduler
import reactivity.experimental.Solo
import java.util.concurrent.CompletableFuture

/**
 * Jdk8 Builder for [Solo], a single (or empty) value Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
object SoloJdk8Builder {

    /**
     * Creates a [Solo] from a [CompletableFuture]
     *
     * @return the [Solo]<T> created
     *
     * @param T the type of the input [completableFuture]
     */
    @JvmStatic
    fun <T> fromCompletableFuture(completableFuture: CompletableFuture<T>) = completableFuture.toSolo()

    /**
     * Creates a [Solo] from a [CompletableFuture]
     *
     * @return the [Solo]<T> created
     *
     * @param T the type of the input [completableFuture]
     */
    @JvmStatic
    fun <T> fromCompletableFuture(scheduler: Scheduler, completableFuture: CompletableFuture<T>) = completableFuture.toSolo(scheduler)
}

object CompletableFutureFromSolo {

    /**
     * Creates a [CompletableFuture] from a [Solo]
     *
     * @return the [CompletableFuture]<T> created
     *
     * @param T the type of the input [Solo]
     */
    @JvmStatic
    fun <T> build(han: Solo<T>) = han.toCompletableFuture()

    /**
     * Creates a [CompletableFuture] from a [Solo]
     *
     * @return the [CompletableFuture]<T> created
     *
     * @param T the type of the input [Solo]
     */
    @JvmStatic
    fun <T> build(scheduler: Scheduler, han: Solo<T>) = han.toCompletableFuture(scheduler)
}