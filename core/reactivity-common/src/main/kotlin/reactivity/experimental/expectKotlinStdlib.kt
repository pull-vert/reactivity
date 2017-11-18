package reactivity.experimental

expect inline fun <T : Closeable?, R> T.use(block: (T) -> R): R