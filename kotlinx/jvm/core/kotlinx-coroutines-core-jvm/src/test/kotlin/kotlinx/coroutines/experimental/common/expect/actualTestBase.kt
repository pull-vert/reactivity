package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.DefaultExecutor
import kotlinx.coroutines.experimental.checkTestThreads
import kotlinx.coroutines.experimental.common.AbstractTestBase
import kotlinx.coroutines.experimental.currentThreads

actual open class TestBase actual constructor(): AbstractTestBase() {
    /**
     * Is `true` when nightly stress test is done.
     */
    val isStressTest = System.getProperty("stressTest") != null
    /**
     * Multiply various constants in stress tests by this factor, so that they run longer during nightly stress test.
     */
    val stressTestMultiplier = if (isStressTest) 30 else 1
    private lateinit var threadsBefore: Set<Thread>

    actual override fun platformBefore() {
        CommonPool.usePrivatePool()
        threadsBefore = currentThreads()
    }

    actual override fun platformOnCompletion() {
        CommonPool.shutdown(SHUTDOWN_TIMEOUT)
        DefaultExecutor.shutdown(SHUTDOWN_TIMEOUT)
        checkTestThreads(threadsBefore)
    }

}