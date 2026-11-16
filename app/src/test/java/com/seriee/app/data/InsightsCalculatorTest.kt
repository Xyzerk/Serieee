package com.seriee.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsCalculatorTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: Long = Instant.parse("2026-11-16T12:00:00Z").toEpochMilli()
    private val today: LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    private val day = 86_400_000L

    private fun item(
        id: String,
        type: MediaType = MediaType.Series,
        status: WatchStatus = WatchStatus.Watching,
        currentEp: Int = 0,
        totalEp: Int = 1,
        rating: Int = 0,
        tags: List<String> = emptyList()
    ) = MediaItem(
        id = id,
        title = id,
        type = type,
        status = status,
        poster = "",
        currentEp = currentEp,
        totalEp = totalEp,
        rating = rating,
        tags = tags,
        updatedAt = now
    )

    private fun event(
        itemId: String = "a",
        season: Int = 1,
        episode: Int = 1,
        count: Int = 1,
        watchedAt: Long
    ) = WatchEvent(itemId, "Title", MediaType.Series, season, episode, count, watchedAt)

    @Test
    fun emptyLibraryIsSafe() {
        val result = InsightsCalculator.calculate(emptyList(), emptyList(), now = now, zone = zone)

        assertEquals(0, result.totalTitles)
        assertEquals(0, result.watchedEpisodes)
        assertEquals(0, result.completionRate)
        assertEquals(0f, result.averageRating)
        assertEquals(WatchStatus.entries.toList(), result.statusCounts.keys.toList())
        assertTrue(result.statusCounts.values.all { it == 0 })
        assertEquals(List(6) { 0 }, result.ratingCounts)
        assertTrue(result.dailyCounts.isEmpty())
        assertTrue(result.recent.isEmpty())
    }

    @Test
    fun seriesAndMoviesAreCountedSeparately() {
        val items = listOf(
            item("series-1", currentEp = 3, totalEp = 10),
            item("series-2", currentEp = 2, totalEp = 2),
            item("movie-1", type = MediaType.Movie, currentEp = 1, totalEp = 1)
        )

        val result = InsightsCalculator.calculate(items, emptyList(), now = now, zone = zone)

        assertEquals(3, result.totalTitles)
        assertEquals(2, result.totalSeries)
        assertEquals(1, result.totalMovies)
        assertEquals(5, result.watchedEpisodes)
        assertEquals(1, result.moviesWatched)
        assertEquals(5 * 24, result.estimatedMinutes)
    }

    @Test
    fun notStartedTitlesDoNotCountWatchedEpisodes() {
        val items = listOf(
            item("soon", status = WatchStatus.ComingSoon, currentEp = 1, totalEp = 24),
            item("planned", status = WatchStatus.PlanToWatch, currentEp = 1, totalEp = 12),
            item("watching", status = WatchStatus.Watching, currentEp = 3, totalEp = 10),
            item(
                "movie",
                type = MediaType.Movie,
                status = WatchStatus.ComingSoon,
                currentEp = 1,
                totalEp = 1
            )
        )

        val result = InsightsCalculator.calculate(items, emptyList(), now = now, zone = zone)

        assertEquals(3, result.watchedEpisodes)
        assertEquals(0, result.moviesWatched)
        assertEquals(3 * 24, result.estimatedMinutes)
    }

    @Test
    fun completionRateRoundsDownToWholePercent() {
        val items = listOf(
            item("a", status = WatchStatus.Completed),
            item("b"),
            item("c")
        )

        val result = InsightsCalculator.calculate(items, emptyList(), now = now, zone = zone)

        assertEquals(1, result.completedTitles)
        assertEquals(33, result.completionRate)
    }

    @Test
    fun averageRatingIgnoresUnratedTitles() {
        val items = listOf(item("a", rating = 5), item("b", rating = 3), item("c", rating = 0))

        val result = InsightsCalculator.calculate(items, emptyList(), now = now, zone = zone)

        assertEquals(4.0f, result.averageRating)
        assertEquals(2, result.ratedTitles)
        assertEquals(1, result.ratingCounts[5])
        assertEquals(1, result.ratingCounts[3])
        assertEquals(0, result.ratingCounts[0])
    }

    @Test
    fun topTagsAreCaseInsensitiveAndKeepFirstCasing() {
        val items = listOf(
            item("a", tags = listOf("Action", "action", "Drama")),
            item("b", tags = listOf("ACTION", "Comedy"))
        )

        val result = InsightsCalculator.calculate(items, emptyList(), now = now, zone = zone)

        assertEquals("Action" to 3, result.topTags.first())
        assertEquals(3, result.topTags.size)
        assertEquals(listOf("Comedy", "Drama"), result.topTags.drop(1).map { it.first })
    }

    @Test
    fun dailyCountsMergeEventsAndSeedWatchDaysOnlyWhenMissing() {
        val history = listOf(
            event(count = 2, watchedAt = now),
            event(count = 1, watchedAt = now - day)
        )
        val watchDays = setOf(
            today.toString(),
            today.minusDays(3).toString(),
            today.minusDays(200).toString()
        )

        val result = InsightsCalculator.calculate(
            emptyList(),
            history,
            watchDays = watchDays,
            now = now,
            zone = zone
        )

        assertEquals(2, result.dailyCounts[today])
        assertEquals(1, result.dailyCounts[today.minusDays(1)])
        assertEquals(1, result.dailyCounts[today.minusDays(3)])
        assertEquals(null, result.dailyCounts[today.minusDays(200)])
    }

    @Test
    fun eventsOutsideActivityWindowAreIgnored() {
        val history = listOf(event(watchedAt = now - 100 * day))

        val result = InsightsCalculator.calculate(emptyList(), history, now = now, zone = zone)

        assertTrue(result.dailyCounts.isEmpty())
    }

    @Test
    fun recentIsNewestFirstAndLimited() {
        val history = (0 until 12).map { index -> event(episode = index, watchedAt = now - index * 1_000L) }

        val result = InsightsCalculator.calculate(emptyList(), history, now = now, zone = zone)

        assertEquals(InsightsCalculator.RECENT_LIMIT, result.recent.size)
        assertEquals(0, result.recent.first().episode)
        assertEquals(9, result.recent.last().episode)
    }
}
