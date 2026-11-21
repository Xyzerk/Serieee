package com.seriee.app.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime

/** Pure countdown math: local calendar days, with hours/minutes floored. */
class CountdownRulesTest {
    private val now = OffsetDateTime.parse("2026-11-21T20:45:00+07:00").toInstant().toEpochMilli()

    @Test
    fun `three calendar dates ahead is three days`() =
        assertEquals(3, CountdownRules.daysCeil(now, OffsetDateTime.parse("2026-11-24T19:00:00+07:00").toInstant().toEpochMilli()))

    @Test
    fun `same day two hours ahead is zero days`() =
        assertEquals(0, CountdownRules.daysCeil(now, now + 2 * 3_600_000L))

    @Test
    fun `past and null targets are zero days`() {
        assertEquals(0, CountdownRules.daysCeil(now, now - 1))
        assertEquals(0, CountdownRules.daysCeil(now, null))
    }

    @Test
    fun `hours and minutes are floored`() {
        assertEquals(2, CountdownRules.hoursFloor(now, now + 2 * 3_600_000L + 59 * 60_000L))
        assertEquals(119, CountdownRules.minutesFloor(now, now + 2 * 3_600_000L - 60_000L))
    }
}
