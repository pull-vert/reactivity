package reactivity.experimental.core

interface MultiPublisherGrouped<T, out R> : MultiPublisher<T> {
    val key : R
}