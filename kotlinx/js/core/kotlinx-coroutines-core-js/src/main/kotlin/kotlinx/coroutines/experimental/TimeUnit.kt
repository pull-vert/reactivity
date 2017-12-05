package kotlinx.coroutines.experimental

// Scales as constants
private val MILLI_SCALE = 1L
private val SECOND_SCALE = 1000L * MILLI_SCALE
private val MINUTE_SCALE = 60L * SECOND_SCALE
private val HOUR_SCALE = 60L * MINUTE_SCALE
private val DAY_SCALE = 24L * HOUR_SCALE

enum class TimeUnit constructor(s: Long) {
    MILLISECONDS(MILLI_SCALE),
    SECONDS(SECOND_SCALE),
    MINUTES(MINUTE_SCALE),
    HOURS(HOUR_SCALE),
    DAYS(DAY_SCALE);
}