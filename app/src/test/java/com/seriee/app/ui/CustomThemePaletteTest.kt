package com.seriee.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.seriee.app.data.AppThemeMode
import com.seriee.app.data.AppThemeStyle
import com.seriee.app.data.ThemePreset
import com.seriee.app.data.ThemePresetCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomThemePaletteTest {

    private val accent = Color(0xFFFFDE59)
    private val darkBackground = Color(0xFF101828)
    private val lightBackground = Color(0xFFFFF7D6)

    private fun palette(custom: CustomThemeColors, reduceTransparency: Boolean = false) =
        resolvePalette(
            mode = AppThemeMode.Dark,
            style = AppThemeStyle.Custom,
            custom = custom,
            reduceTransparency = reduceTransparency
        )

    @Test
    fun `solid custom uses chosen background as the base`() {
        val p = palette(CustomThemeColors(accent = accent, backgroundStart = darkBackground))

        assertEquals(darkBackground, p.bg)
        assertFalse(p.isLight)
        assertNull(p.gradient)
        assertFalse(p.isGlass)
        assertFalse(p.isNeoBrutalist)
    }

    @Test
    fun `light background flips text and surfaces to light`() {
        val p = palette(CustomThemeColors(accent = accent, backgroundStart = lightBackground))

        assertTrue(p.isLight)
        assertTrue(p.text.luminance() < 0.5f)
        assertTrue(p.card.luminance() > p.bg.luminance())
    }

    @Test
    fun `gradient exposes both stops`() {
        val end = Color(0xFF7C2A64)
        val p = palette(
            CustomThemeColors(
                accent = accent,
                backgroundStart = darkBackground,
                backgroundEnd = end,
                gradient = true
            )
        )

        assertEquals(listOf(darkBackground, end), p.gradient)
    }

    @Test
    fun `glass surfaces are translucent until transparency is reduced`() {
        val glass = palette(CustomThemeColors(accent = accent, backgroundStart = darkBackground, glass = true))
        assertTrue(glass.isGlass)
        assertTrue(glass.isLiquidGlass)
        assertTrue(glass.card.alpha < 1f)
        assertTrue(glass.surface.alpha < 1f)

        val opaque = palette(
            CustomThemeColors(accent = accent, backgroundStart = darkBackground, glass = true),
            reduceTransparency = true
        )
        assertFalse(opaque.isGlass)
        assertEquals(1f, opaque.card.alpha)
        assertEquals(1f, opaque.surface.alpha)
    }

    @Test
    fun `high contrast adopts neo brutalist tokens but keeps the accent`() {
        val p = palette(
            CustomThemeColors(
                accent = accent,
                backgroundStart = lightBackground,
                highContrast = true
            )
        )

        assertTrue(p.isNeoBrutalist)
        assertTrue(p.isLight)
        assertEquals(lightBackground, p.bg)
        assertEquals(accent, p.primary)
        assertEquals(accent, p.accent)
        assertEquals(Color.Black, p.text)
    }

    @Test
    fun `theme code round-trips glass and high contrast`() {
        val preset = ThemePreset(
            mode = AppThemeMode.Dark,
            style = AppThemeStyle.Custom,
            gradient = true,
            glass = true,
            highContrast = false
        )
        val decoded = ThemePresetCodec.decode(ThemePresetCodec.encode(preset))

        assertEquals(preset, decoded)
    }

    @Test
    fun `older theme codes default the new fields to off`() {
        val legacy = "seriee-theme:1;mode=Dark;style=Custom;accent=FFFFDE59;bg1=FF101828;bg2=FF7C2A64;gradient=1;flat=0"
        val decoded = ThemePresetCodec.decode(legacy)

        assertFalse(decoded!!.glass)
        assertFalse(decoded.highContrast)
    }
}
