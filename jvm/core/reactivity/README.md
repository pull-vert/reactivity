# Module reactivity

## Setup reactivity in your JVM project

### With gradle

To use this library, you must add the specific repository :

```groovy
repositories {
    maven { setUrl("https://dl.bintray.com/pull-vert/reactivity/") }
}
```

Only one dependency is required :

```groovy
compile "io.reactivity:reactivity:0.0.1"
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

