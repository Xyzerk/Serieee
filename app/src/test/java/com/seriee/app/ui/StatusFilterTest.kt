package com.seriee.app.ui

import com.seriee.app.data.MediaItem
import com.seriee.app.data.MediaType
import com.seriee.app.data.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** `LibraryFilter`: statuses, plus the derived automatic `Ongoing` entry. */
class StatusFilterTest {

    private fun item(status: WatchStatus, airing: Boolean = false, started: Boolean = false): MediaItem =
        MediaItem(id = "x", title = "T", type = MediaType.Series, status = status, poster = "", airing = airing, airingStarted = started)

    @Test
    fun `ongoing filter matches airing shows in any status`() {
        val watching = item(WatchStatus.Watching, airing = true, started = true)
        val completed = item(WatchStatus.Completed, airing = true, started = true)
        assertTrue(LibraryFilter.Ongoing.matches(watching))
        assertTrue(LibraryFilter.Ongoing.matches(completed))
    }

    @Test
    fun `ongoing count excludes finished shows`() {
        val list = listOf(
            item(WatchStatus.Watching, airing = true, started = true),
            item(WatchStatus.Watching, airing = false, started = true),
        )
        assertEquals(1, list.countMatching(LibraryFilter.Ongoing))
    }

    @Test
    fun `status counts still sum to all`() {
        val list = listOf(
            item(WatchStatus.Watching),
            item(WatchStatus.Completed),
            item(WatchStatus.ComingSoon),
        )
        assertEquals(list.size, list.countMatching(LibraryFilter.All))
        assertEquals(1, list.countMatching(LibraryFilter.Completed))
        assertEquals(1, list.countMatching(LibraryFilter.ComingSoon))
    }

    @Test
    fun `all filter matches everything`() {
        assertTrue(LibraryFilter.All.matches(item(WatchStatus.Dropped)))
    }

    @Test
    fun `ongoing overlaps Watching while the show is airing`() {
        val watchingAiring = item(WatchStatus.Watching, airing = true, started = true)
        assertTrue(LibraryFilter.Watching.matches(watchingAiring))
        assertTrue(LibraryFilter.Ongoing.matches(watchingAiring))
    }
}
