package reactivity.experimental.expect

import reactivity.experimental.ISolo
import reactivity.experimental.ISoloImpl

expect abstract class Solo<T>() : ISolo<T>

expect abstract class ASolo<T>() : Solo<T>, ISoloImpl<T>