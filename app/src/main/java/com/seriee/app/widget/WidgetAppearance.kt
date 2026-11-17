package com.seriee.app.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import com.seriee.app.data.AppThemeMode
import com.seriee.app.data.AppThemeStyle
import com.seriee.app.data.SerieeRepository
import com.seriee.app.ui.CustomThemeColors
import com.seriee.app.ui.SeriePalette
import com.seriee.app.ui.resolvePalette

/**
 * Widget-only appearance helpers.
 *
 * RemoteViews cannot tint shape drawables, so surfaces are painted into small
 * bitmaps with the same palette the app is currently using. That keeps the
 * widget in sync with every style: Classic, AMOLED, glass, custom and
 * Neo-Brutalism (including its hard offset shadow).
 */

/** Resolves the palette the home-screen widget should mirror. */
internal fun loadWidgetPalette(context: Context): SeriePalette {
    val repo = SerieeRepository(context)
    val mode = repo.loadThemeMode()
    val style = repo.loadThemeStyle()
    val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val dark = when (mode) {
        AppThemeMode.System -> systemDark
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }
    val dynamicScheme = if (style == AppThemeStyle.MaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }
    return resolvePalette(
        mode = mode,
        style = style,
        systemDark = systemDark,
        custom = CustomThemeColors(
            accent = Color(repo.loadCustomAccent()),
            backgroundStart = Color(repo.loadCustomBackgroundStart()),
            backgroundEnd = Color(repo.loadCustomBackgroundEnd()),
            gradient = repo.loadCustomGradient(),
            glass = repo.loadCustomGlass(),
            highContrast = repo.loadCustomHighContrast(),
        ),
        dynamicScheme = dynamicScheme,
        reduceTransparency = repo.loadReduceTransparency(),
    )
}

/**
 * Draws a rounded rectangle into a bitmap so it can be used as a RemoteViews
 * image background. [shadow] adds the hard bottom-right offset used by
 * Neo-Brutalism; translucent [fill] values keep glass surfaces see-through.
 */
internal fun widgetSurface(
    widthPx: Int,
    heightPx: Int,
    fill: Int,
    border: Int?,
    borderPx: Float,
    radiusPx: Float,
    shadow: Int? = null,
    shadowOffsetPx: Float = 0f,
): Bitmap {
    val width = widthPx.coerceAtLeast(2)
    val height = heightPx.coerceAtLeast(2)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val offset = if (shadow != null) shadowOffsetPx else 0f
    val right = width - offset
    val bottom = height - offset

    if (shadow != null && offset > 0f) {
        paint.color = shadow
        canvas.drawRoundRect(RectF(offset, offset, width.toFloat(), height.toFloat()), radiusPx, radiusPx, paint)
    }

    paint.color = fill
    canvas.drawRoundRect(RectF(0f, 0f, right, bottom), radiusPx, radiusPx, paint)

    if (border != null && borderPx > 0f) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderPx
        paint.color = border
        val inset = borderPx / 2f
        canvas.drawRoundRect(
            RectF(inset, inset, right - inset, bottom - inset),
            radiusPx,
            radiusPx,
            paint,
        )
    }
    return bitmap
}
