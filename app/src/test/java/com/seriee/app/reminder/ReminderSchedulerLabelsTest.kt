package com.seriee.app.reminder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.OffsetDateTime
import java.util.TimeZone

/** Countdown labels use calendar days and keep the hours branch for same-day targets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderSchedulerLabelsTest {
    private val now = OffsetDateTime.parse("2026-11-21T20:45:00+07:00").toInstant().toEpochMilli()
    private val at = OffsetDateTime.parse("2026-11-24T19:00:00+07:00").toInstant().toEpochMilli()
    private lateinit var context: Context

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jakarta"))
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `airing in three days`() =
        assertEquals("Airing in 3 days", ReminderScheduler.timeUntilRelease(context, at, now))

    @Test
    fun `same day falls through to hours`() =
        assertEquals("Airing in 2 hours", ReminderScheduler.timeUntilRelease(context, now + 2 * 3_600_000L, now))

    @Test
    fun `short badge uses three days`() =
        assertEquals("D-3", ReminderScheduler.countdownShortBadge(context, at, now))

    @Test
    fun `long badge uses three days`() =
        assertEquals("3 Days Left", ReminderScheduler.countdownBadgeText(context, at, now))
}
