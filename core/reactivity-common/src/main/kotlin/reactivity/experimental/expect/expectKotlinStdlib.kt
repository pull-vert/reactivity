package reactivity.experimental.expect

expect inline fun <T : Closeable?, R> T.useCloseable(block: (T) -> R): R