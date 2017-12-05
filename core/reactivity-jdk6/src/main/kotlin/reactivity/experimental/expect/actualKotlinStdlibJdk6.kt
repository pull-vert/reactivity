package reactivity.experimental.expect

actual inline fun <T : Closeable?, R> T.useCloseable(block: (T) -> R): R = this@useCloseable.use(block)