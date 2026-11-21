package com.seriee.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.OffsetDateTime

/** TVmaze schedule: the next episode that has not aired yet, not the next premiere. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TvmazeScheduleTest {

    private fun ep(season: Int, number: Int, airTime: OffsetDateTime): JSONObject =
        JSONObject().put("season", season).put("number", number).put("airstamp", airTime.toString())

    @Test
    fun `next future episode is reported with season and number`() {
        val past = OffsetDateTime.parse("2026-11-20T15:00:00+00:00")
        val future = OffsetDateTime.parse("2026-11-27T15:00:00+00:00")
        val now = past.plusDays(1).toInstant().toEpochMilli()
        val s = parseTvmazeSchedule(
            JSONArray(arrayOf(ep(3, 8, past), ep(3, 9, future))), now,
        )
        assertEquals(9, s.nextEpisode)
        assertEquals(3, s.nextEpisodeSeason)
        assertEquals(future.toInstant().toEpochMilli(), s.nextAirAt)
        assertEquals(1, s.airedCount)
    }

    @Test
    fun `aired episodes count toward airingStarted`() {
        val future = OffsetDateTime.parse("2026-11-27T15:00:00+00:00")
        val s = parseTvmazeSchedule(
            JSONArray(arrayOf(ep(3, 8, OffsetDateTime.parse("2026-11-20T15:00:00+00:00")), ep(3, 9, future))),
            future.toInstant().minusSeconds(3600).toEpochMilli(),
        )
        val info = tvmazeAiring("Running", s)
        assertTrue(info.airing)
        assertTrue(info.airingStarted)
        assertEquals(9, info.nextEpisode)
    }

    @Test
    fun `nothing aired yet means coming soon`() {
        val future = OffsetDateTime.parse("2026-11-27T15:00:00+00:00")
        val s = parseTvmazeSchedule(JSONArray(arrayOf(ep(3, 1, future))), future.toInstant().minusSeconds(3600).toEpochMilli())
        val info = tvmazeAiring("Running", s)
        assertFalse(info.airingStarted)
        assertTrue(!info.airingStarted && info.nextAirAt != null)
    }

    @Test
    fun `past episodes do not count as next`() {
        val past = OffsetDateTime.parse("2026-11-20T15:00:00+00:00")
        val s = parseTvmazeSchedule(JSONArray(arrayOf(ep(3, 8, past), ep(3, 9, past))), past.plusSeconds(10).toInstant().toEpochMilli())
        assertEquals(2, s.airedCount)
        assertNull(s.nextEpisode)
        assertNull(s.nextAirAt)
    }
}
