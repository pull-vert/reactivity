package reactivity.experimental

import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext

// -------------- Interface definitions

interface Solo<out E> {
    suspend fun await(): E
}

// cold Solo Coroutine

fun <T> solo(
        context: CoroutineContext = DefaultDispatcher,
        parent: Job? = null,
        block: suspend CoroutineScope.() -> T
): Solo<T> = object : Solo<T> {
    override suspend fun await(): T {
        return async(context, CoroutineStart.DEFAULT, parent, block).await()
    }
}

// -------------- Top level extensions

fun <E> E.toSolo() = object : Solo<E> {
    override suspend fun await(): E = this@toSolo
}

fun <E> Deferred<E>.toSolo() = object : Solo<E> {
    override suspend fun await(): E = this@toSolo.await()
}

// -------------- Terminal (final/consuming) operations

/**
 * Performs the given [action] for the unique element.
 */
suspend inline fun <T> Solo<T>.consumeUnique(crossinline action: (T) -> Unit) {
    action(this@consumeUnique.await())
}

fun <E> Solo<E>.toDeferred(coroutineContext: CoroutineContext = DefaultDispatcher) = async(coroutineContext) {
    return@async  this@toDeferred.await()
}

// -------------- Intermediate (transforming) operations

fun <E> Solo<E>.delay(time: Int) = object : Solo<E> {
    override suspend fun await(): E {
        kotlinx.coroutines.experimental.delay(time)
        return this@delay.await()
    }
}

/**
 * Returns a [Solo] that uses the [mapper] to transform the element from [E]
 * to [F] and then send it when transformation is done
 *
 * @param mapper the mapper function
 */
fun <E, F> Solo<E>.map(mapper: (E) -> F) = object : Solo<F> {
    override suspend fun await() = mapper(this@map.await())
}