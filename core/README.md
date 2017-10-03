# reactivity

Reactivity is a [Reactive Streams](http://www.reactive-streams.org/) implementation on the JVM using Kotlin language.

The asynchronism is based on suspending functions in coroutines, using [Kotlinx coroutines reactive](https://github.com/Kotlin/kotlinx.coroutines/tree/master/reactive/kotlinx-coroutines-reactive)

For JDK6, use reactivity-jdk6
For JDK7, use reactivity-jdk7
For JDK8, use reactivity-jdk8

**Inspirations :**<br />
[Kotlinx coroutines reactive guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/reactive/coroutines-guide-reactive.md)<br />
[Kotlinx coroutines guide (channels)](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#channel-basics)