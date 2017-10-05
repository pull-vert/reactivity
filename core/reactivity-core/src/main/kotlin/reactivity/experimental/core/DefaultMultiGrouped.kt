package reactivity.experimental.core

interface DefaultMultiGrouped<T, out R> : DefaultMulti<T> {
    val key : R
}