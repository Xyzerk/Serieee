package com.seriee.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Airing fields survive storage; imports never trust a foreign `airingCheckedAt`. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SerieeRepositoryAiringTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repo = SerieeRepository(context)

    @Test
    fun `airing fields round-trip`() {
        val item = MediaItem(
            id = "1", title = "T", type = MediaType.Series, status = WatchStatus.Watching, poster = "",
            airing = true, airingStarted = true, nextEpisode = 9, nextEpisodeSeason = 3,
            nextAirAt = 1_795_788_000_000, airingCheckedAt = 1_795_700_000_000,
        )
        repo.saveItems(listOf(item))
        assertEquals(listOf(item), repo.loadItems())
    }

    @Test
    fun `missing airing keys default for old json`() {
        val old = """[{"id":"1","title":"T","type":"Series","status":"Watching","poster":"","updatedAt":1}]"""
        context.getSharedPreferences("seriee", Context.MODE_PRIVATE)
            .edit().putString("seriee_data_v2", old).commit()
        val item = repo.loadItems().single()
        assertFalse(item.airing)
        assertNull(item.nextAirAt)
        assertEquals(0L, item.airingCheckedAt)
    }

    @Test
    fun `import resets airingCheckedAt`() {
        val json = """{"version":4,"items":[{"id":"1","title":"T","type":"Series","status":"Watching","poster":"","airing":true,"airingStarted":true,"nextAirAt":1795788000000,"airingCheckedAt":1795700000000}]}"""
        assertEquals(0L, repo.importBackupJson(json).items.single().airingCheckedAt)
    }
}
