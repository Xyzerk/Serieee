package com.seriee.app.data

import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AniList mapping: `Page` returns every season, and airing fields are per-entry. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AniListAiringTest {

    private fun media(
        id: Int, title: String, status: String,
        nextEpisode: Int? = null, totalEpisodes: Int? = null,
        startYear: Int? = null, startMonth: Int? = null, startDay: Int? = null,
    ): JSONObject {
        val o = JSONObject().apply {
            put("id", id)
            put("title", JSONObject().apply { put("english", title); put("romaji", title) })
            put("status", status)
            if (totalEpisodes != null) put("episodes", totalEpisodes)
            if (startYear != null) {
                put("startDate", JSONObject().apply {
                    put("year", startYear)
                    put("month", startMonth ?: 1)
                    put("day", startDay ?: 1)
                })
            }
            if (nextEpisode != null) {
                put("nextAiringEpisode", JSONObject().apply {
                    put("episode", nextEpisode)
                    put("airingAt", 1795788000)
                })
            }
        }
        return o
    }

    @Test
    fun `page results keep every season`() {
        val json = JSONObject("""{"data":{"Page":{"media":[
            {"id":161645,"title":{"english":"The Apothecary Diaries","romaji":"The Apothecary Diaries"},"format":"TV","episodes":24,"status":"FINISHED","startDate":{"year":2023,"month":10,"day":2}},
            {"id":176301,"title":{"english":"The Apothecary Diaries Season 2","romaji":"The Apothecary Diaries Season 2"},"format":"TV","episodes":24,"status":"FINISHED","startDate":{"year":2025,"month":1,"day":10}},
            {"id":195516,"title":{"english":"The Apothecary Diaries Season 3","romaji":"The Apothecary Diaries Season 3"},"format":"TV","episodes":12,"status":"RELEASING","startDate":{"year":2026,"month":10,"day":2},"nextAiringEpisode":{"episode":9,"airingAt":1795788000}}
        ]}}}""")
        val results = parseAniListPage(json, "Kusuriya no Hitorigoto")
        assertEquals(listOf("161645", "176301", "195516"), results.map { it.sourceId })
        assertEquals(listOf(1, 2, 3), results.map { it.nextEpisodeSeason })
    }

    @Test
    fun `releasing entry exposes next episode 9`() {
        val r = mapAniListMedia(media(195516, "The Apothecary Diaries Season 3", "RELEASING", nextEpisode = 9, totalEpisodes = 12), "Kusuriya")!!
        assertEquals("195516", r.sourceId)
        assertTrue(r.airing)
        assertTrue(r.airingStarted)
        assertEquals(9, r.nextEpisode)
        assertEquals(3, r.nextEpisodeSeason)
        assertEquals(1795788000000L, r.nextAirAt)
        assertFalse(r.upcoming)
    }

    @Test
    fun `not yet released entry is coming soon`() {
        val r = mapAniListMedia(media(2, "Brand New Show", "NOT_YET_RELEASED", nextEpisode = 1, startYear = 2027, startMonth = 1, startDay = 2), "Brand")!!
        assertFalse(r.airing)
        assertFalse(r.airingStarted)
        assertTrue(r.upcoming)
        assertEquals(1, r.nextEpisode)
        assertEquals(1795788000000L, r.nextAirAt)
    }

    @Test
    fun `finished entry is not airing`() {
        val r = mapAniListMedia(media(161645, "The Apothecary Diaries", "FINISHED", totalEpisodes = 24, startYear = 2023), "Kusuriya")!!
        assertFalse(r.airing)
        assertTrue(r.airingStarted)
        assertFalse(r.upcoming)
        assertEquals(null, r.nextAirAt)
    }
}
