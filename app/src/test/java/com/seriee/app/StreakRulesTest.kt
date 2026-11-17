package com.seriee.app

import com.seriee.app.data.StreakRules
import com.seriee.app.data.StreakState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakRulesTest {

    private val today = LocalDate.of(2026, 1, 10)

    private fun empty() = StreakState(streakDays = 0, bestStreak = 0, lastWatchDay = "", watchDays = emptySet())

    @Test
    fun firstWatch_startsStreak() {
        val next = StreakRules.advance(empty(), today)

        assertEquals(1, next.streakDays)
        assertEquals(1, next.bestStreak)
        assertEquals(today.toString(), next.lastWatchDay)
        assertEquals(setOf(today.toString()), next.watchDays)
    }

    @Test
    fun sameDay_doesNotIncreaseStreak() {
        val state = empty().copy(
            streakDays = 4,
            bestStreak = 6,
            lastWatchDay = today.toString(),
            watchDays = setOf(today.toString()),
        )

        val next = StreakRules.advance(state, today)

        assertEquals(4, next.streakDays)
        assertEquals(6, next.bestStreak)
    }

    @Test
    fun consecutiveDay_increasesAndTracksBest() {
        val state = empty().copy(
            streakDays = 4,
            bestStreak = 4,
            lastWatchDay = today.minusDays(1).toString(),
            watchDays = setOf(today.minusDays(1).toString()),
        )

        val next = StreakRules.advance(state, today)

        assertEquals(5, next.streakDays)
        assertEquals(5, next.bestStreak)
        assertEquals(setOf(today.minusDays(1).toString(), today.toString()), next.watchDays)
    }

    @Test
    fun gap_resetsStreakButKeepsBest() {
        val state = empty().copy(
            streakDays = 7,
            bestStreak = 9,
            lastWatchDay = today.minusDays(3).toString(),
            watchDays = setOf(today.minusDays(3).toString()),
        )

        val next = StreakRules.advance(state, today)

        assertEquals(1, next.streakDays)
        assertEquals(9, next.bestStreak)
    }

    @Test
    fun watchDays_areTrimmedToTheLastSevenDays() {
        val old = today.minusDays(10).toString()
        val recent = today.minusDays(2).toString()
        val state = empty().copy(
            streakDays = 2,
            bestStreak = 2,
            lastWatchDay = recent,
            watchDays = setOf(old, recent),
        )

        val next = StreakRules.advance(state, today)

        assertTrue(recent in next.watchDays)
        assertTrue(today.toString() in next.watchDays)
        assertTrue(old !in next.watchDays)
    }

    @Test
    fun corruptLastWatchDay_isTreatedAsFirstWatch() {
        val state = empty().copy(streakDays = 12, bestStreak = 12, lastWatchDay = "not-a-date")

        val next = StreakRules.advance(state, today)

        assertEquals(1, next.streakDays)
        assertEquals(12, next.bestStreak)
    }
}
