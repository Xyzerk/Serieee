package com.seriee.app

import com.seriee.app.data.InsightsCalculator
import com.seriee.app.data.MediaItem
import com.seriee.app.data.MediaType
import com.seriee.app.data.WatchEvent
import com.seriee.app.data.WatchStatus
import com.seriee.app.ui.notificationCentreEntries
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vetro P1-P3: pure-logic coverage for the advanced Insights and the notification centre. */
class VetroFeaturesTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: Long = Instant.parse("2026-11-16T12:00:00Z").toEpochMilli()
    private val day = 86_400_000L

    private fun item(
        id: String,
        status: WatchStatus = WatchStatus.Watching,
        currentEp: Int = 0,
        totalEp: Int = 12,
        tags: List<String> = emptyList(),
        updatedAt: Long = now,
        airing: Boolean = false,
        airingStarted: Boolean = false,
        nextAirAt: Long? = null,
        notificationsMuted: Boolean = false
    ) = MediaItem(
        id = id,
        title = id,
        type = MediaType.Series,
        status = status,
        poster = "",
        currentEp = currentEp,
        totalEp = totalEp,
        tags = tags,
        updatedAt = updatedAt,
        airing = airing,
        airingStarted = airingStarted,
        nextAirAt = nextAirAt,
        notificationsMuted = notificationsMuted
    )

    private fun event(count: Int, watchedAt: Long) =
        WatchEvent("a", "Title", MediaType.Series, 1, 1, count, watchedAt)

    @Test
    fun genreOutcomesCountsFinishedAndDroppedPerTag() {
        val items = listOf(
            item("a", status = WatchStatus.Completed, tags = listOf("Action", "Drama")),
            item("b", status = WatchStatus.Dropped, tags = listOf("Action")),
            item("c", status = WatchStatus.Watching, tags = listOf("Action"))
        )
        val result = InsightsCalculator.calculate(items, emptyList(), now = now, zone = zone)
        val action = result.genreOutcomes.first { it.genre == "Action" }
        assertEquals(1, action.finished)
        assertEquals(1, action.dropped)
        val drama = result.genreOutcomes.first { it.genre == "Drama" }
        assertEquals(1, drama.finished)
        assertEquals(0, drama.dropped)
    }

    @Test
    fun episodesLast90DaysVsAllTime() {
        val history = listOf(
            event(count = 3, watchedAt = now - 10 * day),
            event(count = 2, watchedAt = now - 100 * day)
        )
        val result = InsightsCalculator.calculate(emptyList(), history, now = now, zone = zone)
        assertEquals(3, result.episodesLast90Days)
        assertEquals(5, result.episodesAllTime)
        assertEquals(6, result.monthlyCounts.size)
        assertEquals(3, result.monthlyCounts.last().second)
    }

    @Test
    fun notificationCentreExcludesMutedAndListsAiringAndQuiet() {
        val airing = item(
            "airing", airing = true, airingStarted = true,
            nextAirAt = now + 2 * day, currentEp = 3
        )
        val mutedAiring = item(
            "muted", airing = true, airingStarted = true,
            nextAirAt = now + day, notificationsMuted = true
        )
        val quiet = item("quiet", currentEp = 4, updatedAt = now - 30 * day)
        val fresh = item("fresh", currentEp = 4, updatedAt = now - day)
        val (airingEntries, quietEntries) = notificationCentreEntries(
            listOf(airing, mutedAiring, quiet, fresh), now
        )
        assertEquals(listOf("airing"), airingEntries.map { it.item.id })
        assertEquals(listOf("quiet"), quietEntries.map { it.item.id })
        assertTrue(airingEntries.first().detail.contains("in 2d"))
    }
}
