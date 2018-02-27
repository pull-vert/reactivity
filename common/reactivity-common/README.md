# Module reactivity-common

Common module to work with reactivity (Reactive implementation) in
[mutliplatform Kotlin projects](https://kotlinlang.org/docs/reference/multiplatform.html).

## Setup reactivity-common in your multiplatform project

### With gradle

To use this library, you must add the specific repository :

```groovy
repositories {
    maven { setUrl("https://dl.bintray.com/pull-vert/reactivity/") }
}
```

Only one dependency is required :

```groovy
compile "io.reactivity:reactivity-common:0.0.1"
```

### With maven

TODO explain the steps with maven project

## Code samples

### Multi cold publisher
```kotlin
Multi
    .range(1, N)
    .filter { it.isGood() }
    .fold(0, { a, b -> a + b })
```

Documentation is provided in platform-specific modules:
* [reactivity](../../jvm/core/reactivity/README.md) for Kotlin/JVM.
* [reactivity-js](../../js/reactivity-js/README.md) for Kotlin/JS.
* [reactivity-native](../../native/reactivity-native/README.md) for Kotlin/native (WIP).