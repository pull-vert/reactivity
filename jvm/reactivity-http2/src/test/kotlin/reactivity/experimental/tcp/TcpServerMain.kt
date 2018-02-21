package reactivity.experimental.tcp

import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) = runBlocking {
    TcpServer().run(coroutineContext)
}
