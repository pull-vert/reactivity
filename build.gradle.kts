import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.1.4-3"
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
}

group = "io.http2"
version = "1.0-SNAPSHOT"

repositories {
//    maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap-1.2") }
    maven { setUrl("http://repo.spring.io/milestone") }
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx/") }
    mavenCentral()
    jcenter()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib")

    // dependencies for reactivity
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.18")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:0.18")
    /*compile("org.jetbrains.kotlinx:atomicfu:0.6") {
        exclude module: "kotlin-stdlib-jre8" //exclude by artifact name
    }*/
    // Reactive Streams
    compile("org.reactivestreams:reactive-streams:1.0.1")
    testCompile("org.reactivestreams:reactive-streams-tck:1.0.1")

//    testCompile "org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version"
    testCompile("org.jetbrains.kotlin:kotlin-test-junit")
    testCompile("org.amshove.kluent:kluent:1.27")

    // TODO remove when dev is done, used only for accessing code
    testCompile("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:0.18")
    testCompile("io.projectreactor:reactor-core:3.1.0.M3")
}

/**
 * Enable coroutines.
 */
kotlin {
    experimental.coroutines = Coroutines.ENABLE
}