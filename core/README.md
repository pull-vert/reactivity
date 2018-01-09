# Reactivity Core projects

Reactivity is a [Reactive Streams](http://www.reactive-streams.org/) implementation on the JVM using Kotlin language.

The asynchronism (Event Loop) is based on suspending functions in coroutines, using [Kotlinx coroutines reactive](https://github.com/Kotlin/kotlinx.coroutines/tree/master/reactive/kotlinx-coroutines-reactive)

[ ![Download](https://api.bintray.com/packages/pull-vert/reactivity/reactivity/images/download.svg) ](https://bintray.com/pull-vert/reactivity/reactivity/_latestVersion#files)

This directory contains modules to use for JDK 6 to 9. 

It contains also extras reactivity dependencies to use for special needs :
- reactivity-extra-android
- reactivity-extra-javafx
- reactivity-extra-swing

## Modules

For JDK6 and JDK7, use [reactivity-core](reactivity-core/README.md)<br />
For JDK8 and JDK9, use [reactivity-jdk8](reactivity-jdk8/README.md)<br />

**Inspirations :**<br />
[Kotlinx coroutines reactive guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/reactive/coroutines-guide-reactive.md)<br />
[Kotlinx coroutines guide (channels)](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#channel-basics)