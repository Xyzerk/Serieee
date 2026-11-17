package com.seriee.app.ui

import androidx.compose.ui.graphics.Color
import com.seriee.app.data.AppThemeMode
import com.seriee.app.data.AppThemeStyle
import com.seriee.app.data.CustomThemeExtras
import com.seriee.app.data.ThemePreset
import com.seriee.app.data.ThemePresetCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomThemeExtrasTest {

    private fun palette(extras: CustomThemeExtras) = resolvePalette(
        mode = AppThemeMode.Dark,
        style = AppThemeStyle.Custom,
        custom = CustomThemeColors(backgroundStart = Color(0xFF101828), extras = extras)
    )

    @Test
    fun `no overrides leaves the base palette untouched`() {
        assertEquals(palette(CustomThemeExtras()), palette(CustomThemeExtras()))
        assertFalse(CustomThemeExtras().hasColorOverrides())
    }

    @Test
    fun `colour overrides replace only the chosen tokens`() {
        val base = palette(CustomThemeExtras())
        val p = palette(CustomThemeExtras(text = 0xFFFF0000L, danger = 0xFF00FF00L))

        assertEquals(Color(0xFFFF0000), p.text)
        assertEquals(Color(0xFF00FF00), p.danger)
        assertEquals(base.muted, p.muted)
        assertEquals(base.success, p.success)
    }

    @Test
    fun `surface override also drives elevated surfaces`() {
        val p = palette(CustomThemeExtras(surface = 0xFF223344L))

        assertEquals(Color(0xFF223344), p.surface)
        assertEquals(Color(0xFF223344), p.elevated)
    }

    @Test
    fun `background image makes cards translucent`() {
        val flat = palette(CustomThemeExtras())
        val withImage = palette(
            CustomThemeExtras(imagePath = "/x/bg.jpg", imageEnabled = true, surfaceOpacity = 0.5f)
        )

        assertTrue(withImage.card.alpha < flat.card.alpha)
        // Compose stores alpha in 10 bits, so 0.5 reads back as ~0.5005.
        assertEquals(0.5f, withImage.card.alpha, 0.01f)
    }

    @Test
    fun `disabled image does not change surfaces`() {
        val flat = palette(CustomThemeExtras())
        val off = palette(CustomThemeExtras(imagePath = "/x/bg.jpg", imageEnabled = false))

        assertEquals(flat.card, off.card)
    }

    @Test
    fun `extras survive a json round trip`() {
        val extras = CustomThemeExtras(
            text = 0xFFABCDEFL,
            star = 0xFF123456L,
            imagePath = "/data/bg.jpg",
            imageEnabled = true,
            imageDim = 0.5f,
            imageBlur = 8f,
            surfaceOpacity = 0.6f
        )

        assertEquals(extras, CustomThemeExtras.fromJson(extras.toJson()))
    }

    @Test
    fun `broken json falls back to defaults`() {
        assertEquals(CustomThemeExtras(), CustomThemeExtras.fromJson("{not json"))
        assertEquals(CustomThemeExtras(), CustomThemeExtras.fromJson(null))
    }

    @Test
    fun `theme code carries overrides but never the image`() {
        val preset = ThemePreset(
            style = AppThemeStyle.Custom,
            extras = CustomThemeExtras(
                text = 0xFFABCDEFL,
                imageDim = 0.5f,
                imagePath = "/private/bg.jpg",
                imageEnabled = true
            )
        )

        val code = ThemePresetCodec.encode(preset)
        val decoded = ThemePresetCodec.decode(code)!!

        assertEquals(0xFFABCDEFL, decoded.extras.text)
        assertEquals(0.5f, decoded.extras.imageDim)
        assertNull(decoded.extras.imagePath)
        assertFalse(code.contains("/private"))
    }

    @Test
    fun `older codes without extras still decode`() {
        val legacy = "seriee-theme:1;mode=Dark;style=Custom;accent=FFFFDE59;bg1=FF101828;bg2=FF7C2A64;gradient=1;flat=0"

        assertEquals(CustomThemeExtras(), ThemePresetCodec.decode(legacy)!!.extras)
    }
}
