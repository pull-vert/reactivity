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

package kotlinx.coroutines.experimental

import kotlinx.coroutines.experimental.common.CoroutineId
import kotlinx.coroutines.experimental.common.CoroutineName
import kotlinx.coroutines.experimental.common.expect.getDebug
import kotlin.coroutines.experimental.CoroutineContext

private val DEBUG = getDebug()

/**
 * Executes a block using a given coroutine context.
 */
inline fun <T> withCoroutineContext(context: CoroutineContext, block: () -> T): T {
    val oldName = context.updateThreadContext()
    try {
        return block()
    } finally {
        restoreThreadContext(oldName)
    }
}

@PublishedApi
internal fun CoroutineContext.updateThreadContext(): String? {
    if (!DEBUG) return null
    val coroutineId = this[CoroutineId] ?: return null
    val coroutineName = this[CoroutineName]?.name ?: "coroutine"
    val currentThread = Thread.currentThread()
    val oldName = currentThread.getName()
    currentThread.setName(buildString(oldName.length + coroutineName.length + 10) {
        append(oldName)
        append(" @")
        append(coroutineName)
        append('#')
        append(coroutineId.id)
    })
    return oldName
}

@PublishedApi
internal fun restoreThreadContext(oldName: String?) {
    if (oldName != null) Thread.currentThread().setName(oldName)
}
