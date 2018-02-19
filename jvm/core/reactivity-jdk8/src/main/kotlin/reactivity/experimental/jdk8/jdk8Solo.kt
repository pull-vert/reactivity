package reactivity.experimental.jdk8

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import reactivity.experimental.Solo
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.experimental.CoroutineContext

// -------------- Top level extensions

fun <E> CompletableFuture<E>.toSolo() = object : Solo<E> {
    suspend override fun await(): E = this@toSolo.await()
}

// -------------- Terminal (final/consuming) operations

fun <E> Solo<E>.toCompletableFuture(coroutineContext: CoroutineContext = DefaultDispatcher) = future(coroutineContext) {
    return@future this@toCompletableFuture.await()
}