package com.seriee.app

import com.seriee.app.data.EpisodeProgressRules
import com.seriee.app.data.MediaItem
import com.seriee.app.data.MediaType
import com.seriee.app.data.Season
import com.seriee.app.data.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeProgressRulesTest {

    private fun series(
        currentEp: Int = 1,
        totalEp: Int = 5,
        seasons: List<Season> = emptyList(),
        status: WatchStatus = WatchStatus.Watching,
    ) = MediaItem(
        id = "show",
        title = "Show",
        type = MediaType.Series,
        status = status,
        poster = "",
        season = 1,
        currentEp = currentEp,
        totalEp = totalEp,
        seasons = seasons,
    )

    private fun movie(currentEp: Int = 0) = MediaItem(
        id = "film",
        title = "Film",
        type = MediaType.Movie,
        status = WatchStatus.Watching,
        poster = "",
        currentEp = currentEp,
        totalEp = 1,
    )

    @Test
    fun movie_firstIncrementCompletes() {
        val result = EpisodeProgressRules.increment(movie(), now = 123L)

        assertEquals(1, result!!.episode)
        assertEquals(1, result.item.currentEp)
        assertEquals(1, result.item.totalEp)
        assertEquals(WatchStatus.Completed, result.item.status)
        assertEquals(123L, result.item.updatedAt)
        assertFalse(EpisodeProgressRules.canIncrement(result.item))
    }

    @Test
    fun movie_alreadyWatched_hasNothingToIncrement() {
        val watched = movie(currentEp = 1)

        assertFalse(EpisodeProgressRules.canIncrement(watched))
        assertNull(EpisodeProgressRules.increment(watched))
    }

    @Test
    fun multiSeason_advancesTheFirstIncompleteSeason() {
        val item = series(
            currentEp = 10,
            totalEp = 24,
            seasons = listOf(
                Season(number = 1, currentEp = 10, totalEp = 10),
                Season(number = 2, currentEp = 0, totalEp = 12),
                Season(number = 3, currentEp = 0, totalEp = 2),
            ),
        )

        val result = EpisodeProgressRules.increment(item, now = 1L)!!

        assertEquals(2, result.season)
        assertEquals(1, result.episode)
        assertEquals(1, result.item.seasons[1].currentEp)
        assertEquals(2, result.item.season)
        assertEquals(11, result.item.currentEp)
        assertEquals(24, result.item.totalEp)
        assertEquals(WatchStatus.Watching, result.item.status)
    }

    @Test
    fun lastEpisodeOfLastSeason_completesTheItem() {
        val item = series(
            currentEp = 4,
            totalEp = 5,
            seasons = listOf(Season(number = 1, currentEp = 4, totalEp = 5)),
        )

        val result = EpisodeProgressRules.increment(item)!!

        assertEquals(5, result.item.currentEp)
        assertEquals(WatchStatus.Completed, result.item.status)
        assertFalse(EpisodeProgressRules.canIncrement(result.item))
        assertNull(EpisodeProgressRules.increment(result.item))
    }

    @Test
    fun legacyItemWithoutSeasons_keepsItsShape() {
        val item = series(currentEp = 2, totalEp = 8, seasons = emptyList())

        val result = EpisodeProgressRules.increment(item)!!

        assertEquals(3, result.item.currentEp)
        assertTrue(result.item.seasons.isEmpty())
        assertEquals(1, result.season)
        assertEquals(3, result.episode)
    }

    @Test
    fun zeroTotalSeason_isNeitherIncrementableNorAdvanceable() {
        val item = series(
            currentEp = 0,
            totalEp = 0,
            seasons = listOf(Season(number = 1, currentEp = 0, totalEp = 0)),
        )

        assertFalse(EpisodeProgressRules.canIncrement(item))
        assertNull(EpisodeProgressRules.increment(item))
    }

    @Test
    fun legacyItemAtTheEnd_doesNotAdvance() {
        val item = series(currentEp = 3, totalEp = 3, seasons = emptyList())

        assertFalse(EpisodeProgressRules.canIncrement(item))
        assertNull(EpisodeProgressRules.increment(item))
    }
}
