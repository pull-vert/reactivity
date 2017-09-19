package reactivity.core.experimental

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactivity.core.experimental.internal.util.cancelledSubscription
import reactivity.core.experimental.internal.util.onErrorDropped
import reactivity.core.experimental.internal.util.validateSubscription
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Common functions for [Multi] and [Solo]
 */
interface PublisherCommons<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T>