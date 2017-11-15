package reactivity.experimental.common

import kotlin.annotation.AnnotationTarget.*

@Target(FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
expect annotation class JvmStatic()