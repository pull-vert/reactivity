package kotlinx.coroutines.experimental.common.expect

import kotlinx.coroutines.experimental.common.CoroutineId

private const val DEBUG_PROPERTY_NAME = "kotlinx.coroutines.debug"

actual fun getDebug() : Boolean = run {
    val value = try { System.getProperty(DEBUG_PROPERTY_NAME) }
        catch (e: SecurityException) { null }
    when (value) {
        "auto", null -> CoroutineId::class.java.desiredAssertionStatus()
        "on", "" -> true
        "off" -> false
        else -> error("System property '$DEBUG_PROPERTY_NAME' has unrecognized value '$value'")
    }
}