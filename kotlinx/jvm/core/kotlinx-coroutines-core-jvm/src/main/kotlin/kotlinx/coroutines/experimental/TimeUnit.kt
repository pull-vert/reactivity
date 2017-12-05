package kotlinx.coroutines.experimental

enum class TimeUnit constructor(val delegate: java.util.concurrent.TimeUnit) {
    NANOSECONDS(java.util.concurrent.TimeUnit.NANOSECONDS),
    MICROSECONDS(java.util.concurrent.TimeUnit.MICROSECONDS),
    MILLISECONDS(java.util.concurrent.TimeUnit.MILLISECONDS),
    SECONDS(java.util.concurrent.TimeUnit.SECONDS),
    MINUTES(java.util.concurrent.TimeUnit.MINUTES),
    HOURS(java.util.concurrent.TimeUnit.HOURS),
    DAYS(java.util.concurrent.TimeUnit.DAYS);
}