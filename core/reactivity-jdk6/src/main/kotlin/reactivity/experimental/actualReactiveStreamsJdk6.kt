package reactivity.experimental

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

actual typealias Subscription = Subscription

actual typealias Subscriber<T> = Subscriber<T>

actual typealias Publisher<T> = Publisher<T>