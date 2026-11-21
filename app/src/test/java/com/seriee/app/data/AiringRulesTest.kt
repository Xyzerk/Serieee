package com.seriee.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure rules behind the Ongoing badge and the stale-Coming-Soon repair. */
class AiringRulesTest {

    private val NOW = 1_800_000_000_000L

    private fun item(
        status: WatchStatus = WatchStatus.Watching,
        airing: Boolean = false,
        airingStarted: Boolean = false,
        nextAirAt: Long? = null,
        releaseDate: Long? = null,
        reminder: Boolean = false,
        checkedAt: Long = 1L,
    ) = MediaItem(
        id = "x",
        title = "T",
        type = MediaType.Series,
        status = status,
        poster = "",
        releaseDate = releaseDate,
        reminderEnabled = reminder,
        airing = airing,
        airingStarted = airingStarted,
        nextAirAt = nextAirAt,
        airingCheckedAt = checkedAt,
    )

    private fun assertBadge(airing: Boolean, started: Boolean, nextAirAt: Long?, expected: AiringBadge) {
        assertEquals(expected, AiringRules.badge(item(airing = airing, airingStarted = started, nextAirAt = nextAirAt)))
    }

    @Test
    fun `comingSoon only before an episode has aired`() {
        assertBadge(airing = true, started = false, nextAirAt = NOW, expected = AiringBadge.ComingSoon)
        assertBadge(airing = false, started = false, nextAirAt = null, expected = AiringBadge.None)
    }

    @Test
    fun `ongoing once the next episode is the second or later`() {
        assertBadge(airing = true, started = true, nextAirAt = NOW, expected = AiringBadge.Ongoing)
    }

    @Test
    fun `finished show is neither ongoing nor coming soon`() {
        assertBadge(airing = false, started = true, nextAirAt = null, expected = AiringBadge.None)
    }

    @Test
    fun `ongoing with unknown next episode still gets the badge`() {
        assertBadge(airing = true, started = true, nextAirAt = null, expected = AiringBadge.Ongoing)
    }

    @Test
    fun `manual title with no airing data has no badge`() {
        assertBadge(airing = false, started = false, nextAirAt = null, expected = AiringBadge.None)
    }

    @Test
    fun `currently airing with a past date becomes Watching`() {
        val repaired = AiringRules.repairComingSoon(item(status = WatchStatus.ComingSoon, airing = true, nextAirAt = NOW + 60_000, releaseDate = NOW - 1), NOW)
        assertEquals(WatchStatus.Watching, repaired?.status)
        assertNull(repaired?.releaseDate)
        assertFalse(repaired!!.reminderEnabled)
    }

    @Test
    fun `date equal to the next airtime is treated as schedule-derived`() {
        val at = NOW + 6L * 86_400_000
        assertEquals(
            WatchStatus.Watching,
            AiringRules.repairComingSoon(item(status = WatchStatus.ComingSoon, airing = true, nextAirAt = at + 20_000, releaseDate = at), NOW)?.status,
        )
    }

    @Test
    fun `future user date is not rewritten`() {
        val at = NOW + 6L * 86_400_000
        assertNull(
            AiringRules.repairComingSoon(item(status = WatchStatus.ComingSoon, airing = true, nextAirAt = NOW + 86_400_000, releaseDate = at), NOW),
        )
    }

    @Test
    fun `finished show is not rewritten`() {
        assertNull(AiringRules.repairComingSoon(item(status = WatchStatus.ComingSoon, airing = false, nextAirAt = null, releaseDate = null), NOW))
    }

    @Test
    fun `repair clears reminder`() {
        val repaired = AiringRules.repairComingSoon(item(status = WatchStatus.ComingSoon, airing = true, nextAirAt = null, releaseDate = null, reminder = true), NOW)
        assertFalse(repaired!!.reminderEnabled)
    }

    @Test
    fun `withAiring copies the six fields and stamps the timestamp`() {
        val base = item(airing = false)
        val updated = AiringRules.withAiring(base, AiringInfo(airing = true, airingStarted = true, nextEpisode = 9, nextEpisodeSeason = 3, nextAirAt = NOW), 999L)
        assertEquals(WatchStatus.Watching, updated.status)
        assertTrue(updated.airing)
        assertTrue(updated.airingStarted)
        assertEquals(9, updated.nextEpisode)
        assertEquals(3, updated.nextEpisodeSeason)
        assertEquals(NOW, updated.nextAirAt)
        assertEquals(999L, updated.airingCheckedAt)
    }
}
