package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.AbstractTestBase

actual open class TestBase actual constructor(): AbstractTestBase() {
    actual override fun platformBefore() {
    }

    actual override fun platformOnCompletion() {
    }

}