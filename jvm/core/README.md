# Reactivity core for Kotlin/JVM

Reactivity is a Reactive implementation on the JVM using Kotlin language.

The asynchronism (Event Loop) is based on suspending functions in coroutines, using [Kotlinx coroutines](https://github.com/Kotlin/kotlinx.coroutines)

[ ![Download](https://api.bintray.com/packages/pull-vert/reactivity/reactivity/images/download.svg) ](https://bintray.com/pull-vert/reactivity/reactivity/_latestVersion#files)

This directory contains modules to use for JDK 6 to latest.

## Modules

* For JDK6 and JDK7, use [reactivity](reactivity/README.md) -- reactivity core.
* For JDK8 and JDK9, use [reactivity-jdk8](reactivity-jdk8/README.md) -- additions for JDK8

**Inspirations :**<br />
[Kotlinx coroutines reactive guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/reactive/coroutines-guide-reactive.md)<br />
[Kotlinx coroutines guide (channels)](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#channel-basics)<br />
[streamBenchmarks](https://github.com/elizarov/StreamBenchmarks)