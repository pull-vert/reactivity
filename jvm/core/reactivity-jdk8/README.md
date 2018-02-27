# Reactivity for JDK8

Specific extensions and operators for JDK8 (Stream to Multi, Solo to CompletableFuture...)

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
compile "io.reactivity:reactivity-jdk8:0.0.4"
```

### With maven

TODO explain the steps with maven project

## Code samples

### Solo cold publisher
```kotlin
12
    .toSolo()
    .toCompletableFuture()
```