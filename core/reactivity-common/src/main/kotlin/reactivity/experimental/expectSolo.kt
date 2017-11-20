package reactivity.experimental

expect abstract class Solo<T>() : ISolo<T>

expect abstract class ASolo<T>() : Solo<T>, ISoloImpl<T>