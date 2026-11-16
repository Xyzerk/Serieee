package com.seriee.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchHistoryRulesTest {

    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    private fun event(
        itemId: String = "a",
        season: Int = 1,
        episode: Int = 1,
        watchedAt: Long
    ) = WatchEvent(
        itemId = itemId,
        title = "Title",
        type = MediaType.Series,
        season = season,
        episode = episode,
        watchedAt = watchedAt
    )

    @Test
    fun emptyInputStaysEmpty() {
        assertTrue(WatchHistoryRules.trim(emptyList(), now).isEmpty())
    }

    @Test
    fun newestFirstOrdering() {
        val events = listOf(
            event(episode = 1, watchedAt = now - 2 * day),
            event(episode = 3, watchedAt = now),
            event(episode = 2, watchedAt = now - day)
        )

        assertEquals(listOf(3, 2, 1), WatchHistoryRules.trim(events, now).map { it.episode })
    }

    @Test
    fun capKeepsNewestEvents() {
        val events = (0 until 600).map { index ->
            event(episode = index, watchedAt = now - index * 1_000L)
        }

        val trimmed = WatchHistoryRules.trim(events, now)

        assertEquals(WatchHistoryRules.MAX_EVENTS, trimmed.size)
        assertEquals(now, trimmed.first().watchedAt)
        assertEquals(now - 499_000L, trimmed.last().watchedAt)
    }

    @Test
    fun eventsOlderThanRetentionAreDropped() {
        val events = listOf(
            event(episode = 1, watchedAt = now - 366 * day),
            event(episode = 2, watchedAt = now - 364 * day)
        )

        assertEquals(listOf(2), WatchHistoryRules.trim(events, now).map { it.episode })
    }

    @Test
    fun exactDuplicatesAreRemoved() {
        val events = listOf(
            event(episode = 5, watchedAt = now),
            event(episode = 5, watchedAt = now)
        )

        assertEquals(1, WatchHistoryRules.trim(events, now).size)
    }
}
