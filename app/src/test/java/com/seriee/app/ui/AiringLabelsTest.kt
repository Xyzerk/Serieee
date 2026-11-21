package com.seriee.app.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.seriee.app.data.MediaItem
import com.seriee.app.data.MediaType
import com.seriee.app.data.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.OffsetDateTime

/** Airline decisions: suffixes on cards/widget/detail, badge labels. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiringLabelsTest {

    private val NOW = 1_793_000_000_000L
    private val AT: Long = OffsetDateTime.parse("2026-11-27T05:00:00+07:00").toInstant().toEpochMilli()

    private fun ongoing(nextEpisode: Int? = 9, nextAirAt: Long? = AT): MediaItem =
        MediaItem(
            id = "x", title = "T", type = MediaType.Series, status = WatchStatus.Watching, poster = "",
            airing = true, airingStarted = true, nextEpisode = nextEpisode, nextAirAt = nextAirAt,
        )

    private fun finished(): MediaItem =
        MediaItem(id = "x", title = "T", type = MediaType.Series, status = WatchStatus.Watching, poster = "", airingStarted = true)

    private fun comingSoonItem(nextAirAt: Long? = AT): MediaItem =
        MediaItem(id = "x", title = "T", type = MediaType.Series, status = WatchStatus.ComingSoon, poster = "", nextAirAt = nextAirAt)

    @Test
    fun `suffix shows episode day and time for a future airing`() {
        assertEquals(AiringSuffix(9, AT), airingSuffixFor(ongoing(9, AT), NOW))
    }

    @Test
    fun `suffix episode is known when everything is known`() {
        assertEquals(9, airingSuffixFor(ongoing(9, AT), NOW)?.episode)
    }

    @Test
    fun `suffix is null once next airtime has passed`() {
        assertNull(airingSuffixFor(ongoing(9, NOW - 1), NOW))
    }

    @Test
    fun `suffix is null for a finished show`() {
        assertNull(airingSuffixFor(finished(), NOW))
    }

    @Test
    fun `coming soon suffix shows the date without an episode`() {
        assertEquals(AiringSuffix(null, AT), airingSuffixFor(comingSoonItem(AT), NOW))
    }

    @Test
    fun `formatted suffix matches the device zone`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val zone = TimeZone.getTimeZone("Asia/Jakarta")
        TimeZone.setDefault(zone)
        val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
        sdf.timeZone = zone
        val expectedTime = sdf.format(Date(AT))
        assertEquals("Ep 9 · $expectedTime", formatAiringSuffix(context, AiringSuffix(9, AT)))
    }

    @Test
    fun `coming soon full label renders`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val expectedTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(AT))
        assertEquals("Coming Soon · $expectedTime", airingFullLabel(context, comingSoonItem(AT), NOW))
    }
}
