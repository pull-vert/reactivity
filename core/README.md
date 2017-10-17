# Reactivity Core projects

Reactivity is a [Reactive Streams](http://www.reactive-streams.org/) implementation on the JVM using Kotlin language.

The asynchronism (Event Loop) is based on suspending functions in coroutines, using [Kotlinx coroutines reactive](https://github.com/Kotlin/kotlinx.coroutines/tree/master/reactive/kotlinx-coroutines-reactive)

This directory contains modules to use for JDK 6 to 9.

## Modules

Reactivity core, **must not be used alone, use the one for your JDK version** [reactivity-core](reactivity-core/README.md) 
For JDK6, use [reactivity-jdk6](reactivity-jdk6/README.md)
For JDK7, use [reactivity-jdk7](reactivity-jdk7/README.md)
For JDK8, use [reactivity-jdk8](reactivity-jdk8/README.md)
For JDK9, use [reactivity-jdk9](reactivity-jdk9/README.md)

**Inspirations :**<br />
[Kotlinx coroutines reactive guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/reactive/coroutines-guide-reactive.md)<br />
[Kotlinx coroutines guide (channels)](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#channel-basics)