package com.seriee.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.seriee.app.data.PosterStore
import com.seriee.app.debug.AppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLoggerTest {

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        AppLogger.init(app)
        AppLogger.clearLogs()
        AppLogger.dismissPendingCrash()
    }

    @Test
    fun crashCapture_tracksStreakAndFormatsLogs() {
        val first = AppLogger.recordCrash("main", IllegalStateException("Boom 1"), commitSync = true)
        assertEquals(1, first.consecutiveCount)
        assertEquals("IllegalStateException", first.exceptionType)
        assertTrue(first.stackTrace.contains("Boom 1"))

        val second = AppLogger.recordCrash("worker", RuntimeException("Boom 2"), commitSync = true)
        assertEquals(2, second.consecutiveCount)

        val logText = AppLogger.formatAllLogs()
        assertTrue(logText.contains("Boom 1"))
        assertTrue(logText.contains("Boom 2"))
        assertTrue(logText.contains("Crash[main]"))
        assertEquals(2, AppLogger.logs.value.size)

        val shareFile = AppLogger.writeShareableLogFile(app)
        assertNotNull(shareFile)
        assertTrue(shareFile!!.exists())
        assertTrue(shareFile.readText().contains("Boom 2"))

        AppLogger.dismissPendingCrash()
        assertNull(AppLogger.pendingCrash.value)

        val afterReset = AppLogger.recordCrash("main", IllegalArgumentException("Boom 3"), commitSync = true)
        assertEquals(1, afterReset.consecutiveCount)
    }

    @Test
    fun pendingCrash_survivesReinit() {
        AppLogger.recordCrash("main", IllegalStateException("Persisted"), commitSync = true)

        AppLogger.init(app)

        val restored = AppLogger.pendingCrash.value
        assertNotNull(restored)
        assertEquals("IllegalStateException", restored!!.exceptionType)
        assertEquals("Persisted", restored.message)
    }

    @Test
    fun regularLogs_areStoredAndExportable() {
        AppLogger.i("Test", "info message")
        AppLogger.w("Test", "warn message")
        AppLogger.e("Test", "error message", IllegalStateException("logged"))

        val levels = AppLogger.logs.value.map { it.level }
        assertEquals(3, levels.size)
        assertEquals(3, levels.toSet().size)

        val logText = AppLogger.formatAllLogs()
        assertTrue(logText.contains("info message"))
        assertTrue(logText.contains("warn message"))
        assertTrue(logText.contains("error message"))
    }

    @Test
    fun posterStorePruneUnused_removesOrphanedFiles() {
        val dir = File(app.filesDir, "posters").apply { mkdirs() }
        val active = File(dir, "keep.jpg").apply { writeText("ok") }
        val stale = File(dir, "orphan.jpg").apply { writeText("old") }

        PosterStore.pruneUnused(app, setOf("posters/${active.name}"))

        assertTrue(active.exists())
        assertFalse(stale.exists())

        PosterStore.pruneUnused(app, emptySet())

        assertFalse(active.exists())
    }
}
