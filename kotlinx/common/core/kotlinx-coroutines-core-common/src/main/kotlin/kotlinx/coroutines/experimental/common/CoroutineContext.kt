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

package kotlinx.coroutines.experimental.common

import kotlinx.coroutines.experimental.common.expect.AtomicLong
import kotlinx.coroutines.experimental.common.expect.DefaultDispatcher
import kotlinx.coroutines.experimental.common.expect.getDebug
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext

private val DEBUG = getDebug()

private val COROUTINE_ID = AtomicLong()

// for tests only
internal fun resetCoroutineId() {
    COROUTINE_ID.set(0)
}

/**
 * Creates context for the new coroutine. It installs [DefaultDispatcher] when no other dispatcher nor
 * [ContinuationInterceptor] is specified, and adds optional support for debugging facilities (when turned on).
 *
 * **Debugging facilities:** In debug mode every coroutine is assigned a unique consecutive identifier.
 * Every thread that executes a coroutine has its name modified to include the name and identifier of the
 * currently currently running coroutine.
 * When one coroutine is suspended and resumes another coroutine that is dispatched in the same thread,
 * then the thread name displays
 * the whole stack of coroutine descriptions that are being executed on this thread.
 *
 * Enable debugging facilities with "`kotlinx.coroutines.debug`" system property, use the following values:
 * * "`auto`" (default mode) -- enabled when assertions are enabled with "`-ea`" JVM option.
 * * "`on`" or empty string -- enabled.
 * * "`off`" -- disabled.
 *
 * Coroutine name can be explicitly assigned using [CoroutineName] context element.
 * The string "coroutine" is used as a default name.
 */
public fun newCoroutineContext(context: CoroutineContext): CoroutineContext {
    val debug = if (DEBUG) context + CoroutineId(COROUTINE_ID.incrementAndGet()) else context
    return if (context !== DefaultDispatcher && context[ContinuationInterceptor] == null)
        debug + DefaultDispatcher else debug
}

internal val CoroutineContext.coroutineName: String? get() {
    if (!DEBUG) return null
    val coroutineId = this[CoroutineId] ?: return null
    val coroutineName = this[CoroutineName]?.name ?: "coroutine"
    return "$coroutineName#${coroutineId.id}"
}

class CoroutineId(val id: Long) : AbstractCoroutineContextElement(CoroutineId) {
    companion object Key : CoroutineContext.Key<CoroutineId>
    override fun toString(): String = "CoroutineId($id)"
}
