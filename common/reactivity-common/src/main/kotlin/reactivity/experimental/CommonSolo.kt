package reactivity.experimental

// -------------- Top level extensions

fun <E> E.toSolo() = object : Solo<E> {
    suspend override fun await(): E = this@toSolo
}

// -------------- Interface definitions

interface Solo<out E> {
    suspend fun await(): E
//    companion object Factory
}

// -------------- Factory (initial/producing) operations

//fun <E> Solo.Factory.fromValue(value: E) = object : Solo<E> {
//    suspend override fun await(): E = value
//}

// -------------- Intermediate (transforming) operations

fun <E> Solo<E>.delay(time: Int) = object : Solo<E> {
    override suspend fun await(): E {
        kotlinx.coroutines.experimental.delay(time)
        return this@delay.await()
    }
}

// -------------- Intermediate (transforming) operations

/**
 * Returns a [Solo] that uses the [mapper] to transform the element from [E]
 * to [F] and then send it when transformation is done
 *
 * @param mapper the mapper function
 */
fun <E, F> Solo<E>.map(mapper: (E) -> F) = object : Solo<F> {
    override suspend fun await() = mapper(this@map.await())
}