/*
 * Copyright 2016-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactivity.experimental

import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.await
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SoloPromiseTest : TestBase() {
    @Test
    fun testPromiseResolvedToSolo() = runTest {
        val promise = Promise<String> { resolve, _ ->
            resolve("OK")
        }
        val solo = promise.toSolo()
        assertEquals("OK", solo.await())
    }
    
    @Test
    fun testPromiseRejectedToSolo() = runTest {
        lateinit var promiseReject: (Throwable) -> Unit
        val promise = Promise<String> { _, reject ->
            promiseReject = reject
        }
        val solo = promise.toSolo()
        // reject after converting to deferred to avoid "Unhandled promise rejection" warnings
        promiseReject(TestException("Rejected"))
        try {
            solo.await()
            expectUnreached()
        } catch (e: Throwable) {
            assertTrue(e is TestException)
            assertEquals("Rejected", e.message)
        }
    }

    @Test
    fun testCompletedSoloToPromise() = runTest {
        val solo = solo(coroutineContext, CoroutineStart.UNDISPATCHED) {
            // completed right away
            "OK"
        }
        val promise = solo.toPromise()
        assertEquals("OK", promise.await())
    }

    @Test
    fun testWaitForSoloToPromise() = runTest {
        val solo = solo(coroutineContext) {
            // will complete later
            "OK"
        }
        val promise = solo.toPromise()
        assertEquals("OK", promise.await()) // await yields main thread to deferred coroutine
    }

    @Test
    fun testToPromiseToSolo() = runTest {
        val solo = solo { "OK" }
        val promise = solo.toPromise()
        val s2 = promise.toSolo()
        assertEquals("OK", s2.await())
    }

    private class TestException(message: String) : Exception(message)
}