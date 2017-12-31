package reactivity.experimental

/**
 * This is the interface declaring the publishOn functions
 * for executing the subscriber in other context than the publisher
 * will be implemented in both [Multi] and [Solo]
 */
interface WithPublishOn {
    fun publishOn(delayError: Boolean): WithPublishOn
    fun publishOn(scheduler: Scheduler, delayError: Boolean): WithPublishOn
}