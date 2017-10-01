package kotlinx

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) = runBlocking {
    println("Let's naively sleep for 1 second")
    delay(1000L)
    println("We're still in current EDT!")
}