package com.seriee.app.data

import java.time.LocalDate

/** Persisted daily-streak state. See [StreakRules.advance]. */
data class StreakState(
    val streakDays: Int,
    val bestStreak: Int,
    val lastWatchDay: String,
    val watchDays: Set<String>,
)

/**
 * Daily streak rules shared by the ViewModel and the widget.
 *
 * Watch activity is bucketed by local calendar day: same day = no change,
 * yesterday = +1, any larger gap = reset to 1. [watchDays] keeps the last week
 * of activity for the insights heat-map.
 */
object StreakRules {
    private const val WINDOW_DAYS = 6L

    fun advance(current: StreakState, today: LocalDate = LocalDate.now()): StreakState {
        val todayStr = today.toString()
        val last = runCatching { LocalDate.parse(current.lastWatchDay) }.getOrNull()
        val newStreak = when (last) {
            null -> 1
            today -> current.streakDays
            today.minusDays(1) -> current.streakDays + 1
            else -> 1
        }
        val newBest = maxOf(current.bestStreak, newStreak)
        val cutoff = today.minusDays(WINDOW_DAYS)
        val newDays = (current.watchDays + todayStr)
            .filter { runCatching { LocalDate.parse(it) >= cutoff }.getOrDefault(false) }
            .toSet()
        return StreakState(newStreak, newBest, todayStr, newDays)
    }
}
