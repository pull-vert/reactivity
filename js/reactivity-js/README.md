# Module reactivity-js

## Setup reactivity-common in your JS project

### With gradle

To use this library, you must add the specific repository :

```groovy
repositories {
    maven { setUrl("https://dl.bintray.com/pull-vert/reactivity/") }
}
```

Only one dependency is required :

```groovy
compile "io.reactivity:reactivity-js:0.0.4"
```

### With maven

TODO explain the steps with maven project

## Coroutine builder functions

| **Name**      | **Result**    | **Scope**        | **Description**
| ------------- | ------------- | ---------------- | ---------------
| [multi]       | [Multi]       | [MultiScope]     | Cold producer of a stream of elements
| [solo]        | [Solo]        | [CoroutineScope] | Cold producer a single elements

## Code samples

### Multi cold publisher

```kotlin
Multi
    .range(1, N)
    .filter { it.isGood() }
    .fold(0, { a, b -> a + b })
```

```kotlin
sequenceOf("lower", "case").toMulti()
    // delay each element
    .delay(10)
    .map { it.toUpperCase() }
    // iterate over the source fully
    source.consumeEach { println(it) }
```

### Solo cold publisher

```kotlin
val han = 12.toSolo()
    .map { it.toString() }
    .delay(10)
    .await()
```
