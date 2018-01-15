package srcmanbase

import srcmanbase.SrcManBase.Factory.SUSPENDED

// -------------- Interface definitions

interface Cont<E> {
    fun resume(value: E)
}

typealias UnitOrSuspend = Any  // Unit | SUSPENDED
typealias BooleanOrSuspend = Any  // Boolean | SUSPENDED

interface SrcManBase<out E> {
    fun consume(sink: Sink<E>, cont: Cont<Unit>): UnitOrSuspend
    companion object Factory {
        val SUSPENDED = Any()
    }
}

interface Sink<in E> {
    fun send(item: E, cont: Cont<Unit>): UnitOrSuspend
}

// -------------- Factory (initial/producing) operations

inline fun <E> srcManBase(crossinline action: (Sink<E>, Cont<Unit>) -> UnitOrSuspend): SrcManBase<E> =
    object : SrcManBase<E> {
        override fun consume(sink: Sink<E>, cont: Cont<Unit>) = action(sink, cont)
    }

fun SrcManBase.Factory.range(start: Int, count: Int): SrcManBase<Int> =
    srcManBase<Int> { sink, completion ->
        val sm = object : Cont<Unit> {
            var i = start
            var suspended = false

            override fun resume(value: Unit) {
                while (true) {
                    if (i >= start + count) break
                    val cur = i++
                    if (sink.send(cur, this) === SUSPENDED) {
                        suspended = true
                        break
                    }
                }
            }
        }
        sm.resume(Unit)
        if (sm.suspended) SUSPENDED else Unit
    }

// -------------- Terminal (final/consuming) operations

inline fun <E> SrcManBase<E>.consumeEach(crossinline action: (item: E, cont: Cont<Unit>) -> UnitOrSuspend, cont: Cont<Unit>): UnitOrSuspend =
    consume(object : Sink<E> {
        override fun send(item: E, cont: Cont<Unit>): Any = action(item, cont)
    }, cont)


fun <E, R> SrcManBase<E>.fold(
    initial: R,
    operation: (acc: R, E, Cont<R>) -> Any?, /* R | Suspended */
    completion: Cont<R>
): Any? /* R | Suspended */ {
    val sm = object : Cont<R> {
        var acc = initial

        override fun resume(value: R) {
            acc = value
        }
    }
    val done = object : Cont<Unit> {
        override fun resume(value: Unit) {
            completion.resume(sm.acc)
        }
    }
    val res = consume(object: Sink<E> {
        override fun send(item: E, cont: Cont<Unit>): UnitOrSuspend {
            val value = operation(sm.acc, item, sm)
            if (value === SUSPENDED) return SUSPENDED
            sm.acc = value as R
            return Unit
        }
    }, done)
    if (res === SUSPENDED) return SUSPENDED
    return sm.acc
}

// -------------- Intermediate (transforming) operations

inline fun <E> SrcManBase<E>.filter(crossinline predicate: (E, Cont<Boolean>) -> BooleanOrSuspend) =
    srcManBase<E> { sink, consumeCompletion ->
        this@filter.consume(object : Sink<E> {
            override fun send(item: E, cont: Cont<Unit>): UnitOrSuspend {
                val sm = object : Cont<Boolean> {
                    override fun resume(value: Boolean) {
                        if (value) {
                            if (sink.send(item, cont) === Unit)
                                cont.resume(Unit)
                        } else
                            cont.resume(Unit)
                    }
                }
                val res = predicate(item, sm)
                if (res === SUSPENDED) return SUSPENDED
                if (res == true) return sink.send(item, cont)
                return Unit
            }
        }, consumeCompletion)
    }

// -------------- Helper

fun <R> SrcManBase.Factory.noSuspend(block: (cont: Cont<R>) -> Any?): R {
    val cont = object : Cont<R> {
        override fun resume(value: R) {
            error("Should not happen")
        }
    }
    val res = block(cont)
    assert(res !== SUSPENDED)
    return res as R
}
