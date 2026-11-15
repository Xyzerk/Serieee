package com.seriee.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WatchStatusTest {

    @Test
    fun `status names round-trip through persistence`() {
        WatchStatus.entries.forEach { status ->
            assertEquals(status, WatchStatus.valueOf(status.name))
        }
    }

    @Test
    fun `unknown persisted status falls back to watching`() {
        val parsed = runCatching { WatchStatus.valueOf("Paused") }
            .getOrDefault(WatchStatus.Watching)
        assertEquals(WatchStatus.Watching, parsed)
    }

    @Test
    fun `new statuses are present`() {
        assertNotNull(WatchStatus.valueOf("OnHold"))
        assertNotNull(WatchStatus.valueOf("Dropped"))
    }

    @Test
    fun `theme axes round-trip and legacy names migrate`() {
        AppThemeMode.entries.forEach { mode ->
            assertEquals(mode, AppThemeMode.fromName(mode.name))
        }
        AppThemeStyle.entries.forEach { style ->
            assertEquals(style, AppThemeStyle.fromName(style.name))
        }
        val expected = mapOf(
            "FollowSystem" to (AppThemeMode.System to AppThemeStyle.Classic),
            "Dark" to (AppThemeMode.Dark to AppThemeStyle.Classic),
            "Amoled" to (AppThemeMode.Dark to AppThemeStyle.Amoled),
            "Glass" to (AppThemeMode.Dark to AppThemeStyle.Frost),
            "LiquidGlass" to (AppThemeMode.Dark to AppThemeStyle.Liquid),
            "Light" to (AppThemeMode.Light to AppThemeStyle.Classic),
            "Custom" to (AppThemeMode.Dark to AppThemeStyle.Custom),
            null to (AppThemeMode.Dark to AppThemeStyle.Classic)
        )
        expected.forEach { (legacy, axes) ->
            assertEquals(axes, LegacyThemeMap.migrate(legacy))
        }
    }

    @Test
    fun `theme codes survive a clipboard round trip`() {
        val preset = ThemePreset(
            mode = AppThemeMode.Light,
            style = AppThemeStyle.Liquid,
            accent = 0xFFF0559BL,
            backgroundStart = 0xFF0EA5E9L,
            backgroundEnd = 0xFF6366F1L,
            gradient = true,
            reduceTransparency = true
        )
        val encoded = ThemePresetCodec.encode(preset)
        assertEquals(preset, ThemePresetCodec.decode(encoded))
    }

    @Test
    fun `theme codes reject unrelated clipboard text`() {
        assertNull(ThemePresetCodec.decode("hello world"))
        assertNull(ThemePresetCodec.decode(null))
        assertNull(ThemePresetCodec.decode("https://example.com"))
        assertNotNull(ThemePresetCodec.decode(ThemePresetCodec.encode(ThemePreset())))
    }

    @Test
    fun `language lookup keeps old tags and adds system default`() {
        assertEquals(AppLanguage.English, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.Indonesian, AppLanguage.fromTag("in"))
        assertEquals(AppLanguage.System, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.System, AppLanguage.fromTag(null))
    }
}
