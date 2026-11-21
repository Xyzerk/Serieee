package com.seriee.app.reminder

import com.seriee.app.data.MediaItem
import com.seriee.app.data.MediaType
import com.seriee.app.data.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which timestamp an armed reminder should use. */
class ReminderTargetTest {
    private val now = 1_800_000_000_000L

    private fun item(status: WatchStatus, releaseDate: Long?, nextAirAt: Long?, episode: Int? = null) =
        MediaItem(
            id = "x", title = "T", type = MediaType.Series, status = status, poster = "",
            releaseDate = releaseDate, nextAirAt = nextAirAt, nextEpisode = episode,
        )

    @Test
    fun `coming soon uses releaseDate only`() =
        assertEquals(now + 10_000, reminderTarget(item(WatchStatus.ComingSoon, now + 10_000, now + 5_000), now))

    @Test
    fun `coming soon with only nextAirAt has no target`() =
        assertNull(reminderTarget(item(WatchStatus.ComingSoon, null, now + 5_000), now))

    @Test
    fun `ongoing uses nextAirAt`() =
        assertEquals(now + 5_000, reminderTarget(item(WatchStatus.Watching, null, now + 5_000, 9), now))

    @Test
    fun `earliest future date wins`() =
        assertEquals(now + 5_000, reminderTarget(item(WatchStatus.Watching, now + 10_000, now + 5_000), now))

    @Test
    fun `past dates are ignored`() =
        assertNull(reminderTarget(item(WatchStatus.Watching, now - 1, now - 2), now))

    @Test
    fun `finished source has no reminder target`() =
        assertNull(reminderTarget(item(WatchStatus.Watching, null, null), now))
}
