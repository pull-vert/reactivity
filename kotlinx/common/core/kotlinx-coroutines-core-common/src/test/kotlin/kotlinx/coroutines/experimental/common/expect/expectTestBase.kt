package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.AbstractTestBase

expect open class TestBase(): AbstractTestBase {
    override fun platformBefore()
    override fun platformOnCompletion()
}