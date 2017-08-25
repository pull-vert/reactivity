package reactivity.experimental

/**
 * Represents a disposable resource.
 */
interface Disposable {

    /**
     * Optionally return true when the resource or task is disposed.
     *
     *
     * Implementations are not required to track disposition and as such may never
     * return true even when disposed. However, they MUST only return true
     * when there's a guarantee the resource or task is disposed.
     *
     * @return true when there's a guarantee the resource or task is disposed.
     */
    fun isDisposed(): Boolean {
        return false
    }

    /**
     * Dispose the resource, the operation should be idempotent.
     */
    fun dispose()
}