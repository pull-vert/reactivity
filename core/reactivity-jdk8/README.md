# Reactivity for JDK8

Work In Progress, all operators are not implemented yet.

<!--- TOC -->

* [Setup reactivity-jdk8 in your project](#setup-reactivity-jdk8-in-your-project)
  * [With gradle](#with-gradle)
  * [With maven](#with-maven)
* [Code samples](#code-samples)
  * [In Kotlin](#in-kotlin)
  * [In Java](#in-java)

<!--- END_TOC -->

## Setup reactivity-jdk8 in your project

### With gradle

To use this library, you must add the specific repository :

```groovy
repositories {
    maven { setUrl("https://dl.bintray.com/pull-vert/reactivity/") }
}
```

Only one dependency is required :

```groovy
compile "io.reactivity:reactivity-jdk8:0.0.1"
```

### With maven

TODO explain the steps with maven project

## Code samples

### In Kotlin
```kotlin
(1..3).toMulti()
    .map{it * 2}
    .subscribe { x ->
        println("Processed $x")
    }
```
### In Java
```java
Multi.fromRange(1, 3)
    .map((value) -> value * 2)
    .subscribe((value) -> {
        System.out.println("Processed  " + value);
        return null;
    });
```