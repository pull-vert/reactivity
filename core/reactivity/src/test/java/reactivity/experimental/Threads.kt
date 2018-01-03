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

fun currentThreads(): Set<Thread> {
    var estimate = 0
    while (true) {
        estimate = estimate.coerceAtLeast(Thread.activeCount() + 1)
        val arrayOfThreads = Array<Thread?>(estimate) { null }
        val n = Thread.enumerate(arrayOfThreads)
        if (n >= estimate) {
            estimate = n + 1
            continue // retry with a better size estimate
        }
        val threads = hashSetOf<Thread>()
        for (i in 0 until n)
            threads.add(arrayOfThreads[i]!!)
        return threads
    }
}
