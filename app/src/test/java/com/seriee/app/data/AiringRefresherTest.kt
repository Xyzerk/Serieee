package com.seriee.app.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure selection/merge rules behind the once-per-open airing refresh. */
class AiringRefresherTest {

    private val NOW = 1_800_000_000_000L

    private fun info(
        airing: Boolean = true,
        started: Boolean = true,
        nextEpisode: Int? = 9,
        nextAirAt: Long? = NOW + 86_400_000,
    ): AiringInfo = AiringInfo(airing = airing, airingStarted = started, nextEpisode = nextEpisode, nextAirAt = nextAirAt)

    private fun aniItem(id: String): MediaItem = MediaItem(
        id = id, title = "T$id", type = MediaType.Series,
        status = WatchStatus.Watching, poster = "", source = "AniList", sourceId = id,
    )

    private fun tvmazeItem(id: String, nextAirAt: Long? = null, checkedAt: Long = 0L): MediaItem =
        aniItem(id).copy(source = "TVMaze", nextAirAt = nextAirAt, airingCheckedAt = checkedAt)

    @Test
    fun `ani list ids are chunked by fifty`() {
        var calls = 0
        val refresher = AiringRefresher(
            fetchAniList = { ids -> calls++; ids.associateWith { info() } },
            fetchTvmaze = { null },
            now = { NOW },
        )
        runBlocking { refresher.refresh((1..51).map { aniItem("$it") }) }
        assertEquals(2, calls)
    }

    @Test
    fun `non numeric anilist ids are skipped`() {
        val refresher = AiringRefresher(
            fetchAniList = { ids -> ids.forEach { id -> assertTrue(id.toIntOrNull() != null) }; emptyMap() },
            fetchTvmaze = { null },
            now = { NOW },
        )
        val result = runBlocking { refresher.refresh(listOf(aniItem("S3"), aniItem("12"), aniItem("X"))) }
        assertTrue(result.updates.isEmpty())
    }

    @Test
    fun `only returned ids are stamped`() {
        val refresher = AiringRefresher(
            fetchAniList = { mapOf("1" to info()) },
            fetchTvmaze = { null },
            now = { NOW },
        )
        val result = runBlocking { refresher.refresh(listOf(aniItem("1"), aniItem("2"))) }
        assertEquals(setOf("1"), result.updates.keys)
        assertNotNull(result.updates["1"])
        assertEquals(NOW, result.checkedAt)
    }

    @Test
    fun `tv maze is limited to ten stale shows`() {
        val stale = (1..12).map { tvmazeItem("S$it", nextAirAt = NOW - 1) }
        val ids = mutableListOf<String>()
        val refresher = AiringRefresher(
            fetchAniList = { emptyMap() },
            fetchTvmaze = { id -> ids.add(id); AiringInfo(airing = false, airingStarted = true) },
            now = { NOW },
        )
        runBlocking { refresher.refresh(stale) }
        assertEquals(10, ids.size)
    }

    @Test
    fun `stale shows are fetched before fresh ones`() {
        val items = listOf(
            tvmazeItem("A", nextAirAt = NOW - 1, checkedAt = NOW),
            tvmazeItem("B", nextAirAt = NOW + 1000, checkedAt = NOW), // fresh: skipped
            tvmazeItem("C", nextAirAt = null, checkedAt = 0L),         // never checked: included
        )
        val order = mutableListOf<String>()
        val refresher = AiringRefresher(
            fetchAniList = { emptyMap() },
            fetchTvmaze = { id -> order.add(id); null },
            now = { NOW },
        )
        runBlocking { refresher.refresh(items) }
        assertEquals(listOf("A", "C"), order)
    }

    @Test
    fun `repair applies only to refreshed items`() {
        val item = aniItem("1").copy(status = WatchStatus.ComingSoon, airing = false, nextAirAt = null, releaseDate = null)
        val applied = AiringApply.apply(listOf(item), AiringRefreshResult(mapOf("1" to info(airing = true, started = true)), NOW))
        assertEquals(WatchStatus.Watching, applied.items.single().status)
        assertTrue(applied.repairedIds.contains("1"))
        assertTrue(applied.changed)
    }

    @Test
    fun `editing during refresh is not overwritten`() {
        val snapshot = aniItem("1")
        val latest = snapshot.copy(currentEp = 5, title = "Edited")
        val applied = AiringApply.apply(listOf(latest), AiringRefreshResult(mapOf("1" to info()), NOW))
        assertEquals(5, applied.items.single().currentEp)
        assertEquals("Edited", applied.items.single().title)
        assertEquals(9, applied.items.single().nextEpisode)
    }

    @Test
    fun `states of unfinished repairs are idempotent`() {
        val repaired = AiringRules.withAiring(aniItem("1"), info(), NOW).copy(status = WatchStatus.Watching, releaseDate = null, reminderEnabled = false)
        val applied = AiringApply.apply(listOf(repaired), AiringRefreshResult(mapOf("1" to info(airing = true, started = true)), NOW))
        assertTrue(applied.repairedIds.isEmpty())
        assertFalse(applied.changed)
    }

    @Test
    fun `metadata updates replace stored fields without touching progress`() {
        val item = aniItem("1").copy(currentEp = 7)
        val applied = AiringApply.apply(listOf(item), AiringRefreshResult(mapOf("1" to info(nextEpisode = 9)), NOW))
        val out = applied.items.single()
        assertEquals(9, out.nextEpisode)
        assertEquals(7, out.currentEp)
        assertEquals(NOW, out.airingCheckedAt)
    }
}
