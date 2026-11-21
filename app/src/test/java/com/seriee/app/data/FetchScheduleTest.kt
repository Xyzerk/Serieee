package com.seriee.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Rules for `MediaFormDialog.applyFetch`: a result only tags a title
 * "Coming Soon" when it genuinely has not aired; an already-airing result
 * repairs a wrongly-tagged item back to "Watching".
 */
class FetchScheduleTest {

    private val NOW = 1_700_000_000_000L

    private fun result(
        upcoming: Boolean = false,
        releaseAt: Long? = null,
        airingStarted: Boolean = false,
    ): MetadataResult = MetadataResult(
        source = "AniList",
        sourceId = "1",
        title = "T",
        posterUrl = "",
        description = "",
        tags = emptyList(),
        seasonEpisodeCounts = emptyList(),
        streamCandidates = emptyList(),
        upcoming = upcoming,
        releaseAt = releaseAt,
        airingStarted = airingStarted,
    )

    @Test
    fun `not yet released result becomes coming soon`() {
        val s = FetchScheduleRules.resolve(WatchStatus.Watching, result(upcoming = true, releaseAt = 1_800_000_000_000), NOW)
        assertEquals(WatchStatus.ComingSoon, s.status)
        assertEquals(1_800_000_000_000L, s.releaseDate)
        assertEquals(true, s.reminderEnabled)
    }

    @Test
    fun `airing result keeps watching`() {
        assertNull(FetchScheduleRules.resolve(WatchStatus.Watching, result(airingStarted = true), NOW).status)
    }

    @Test
    fun `airing result repairs a coming soon item`() {
        val s = FetchScheduleRules.resolve(WatchStatus.ComingSoon, result(airingStarted = true), NOW)
        assertEquals(WatchStatus.Watching, s.status)
        assertNull(s.releaseDate)
        assertEquals(false, s.reminderEnabled)
    }

    @Test
    fun `released result clears the premiere date when repairing`() {
        val s = FetchScheduleRules.resolve(WatchStatus.ComingSoon, result(airingStarted = true, releaseAt = 1_600_000_000_000), NOW)
        assertNull(s.releaseDate)
    }
}
