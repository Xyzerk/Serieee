package com.seriee.app.reminder

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Pure countdown math. A "day" is a local calendar-date boundary crossing, so
 * a target two hours from now is still zero days and falls through to the
 * hours/minutes labels.
 */
object CountdownRules {
    fun daysCeil(now: Long, target: Long?): Int {
        if (target == null || target <= now) return 0
        val start = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        val end = Instant.ofEpochMilli(target).atZone(ZoneId.systemDefault()).toLocalDate()
        return ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(0)
    }

    fun hoursFloor(now: Long, target: Long?): Int =
        if (target == null || target <= now) 0 else ((target - now) / 3_600_000L).toInt()

    fun minutesFloor(now: Long, target: Long?): Int =
        if (target == null || target <= now) 0 else ((target - now) / 60_000L).toInt()
}
