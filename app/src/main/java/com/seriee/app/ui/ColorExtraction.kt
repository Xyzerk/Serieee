package com.seriee.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Loads a (possibly remote) poster and derives a vivid accent colour from its
 * dominant hue, so the custom theme can follow the artwork.
 */
suspend fun accentFromPoster(context: Context, data: String): Color? =
    withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(data)
            .size(64)
            .allowHardware(false)
            .build()
        val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
        val bitmap = (result?.drawable as? BitmapDrawable)?.bitmap ?: return@withContext null
        dominantAccent(bitmap)
    }

private class HueBucket {
    var count = 0
    var red = 0L
    var green = 0L
    var blue = 0L
    var saturation = 0f
    var value = 0f

    fun add(r: Int, g: Int, b: Int, s: Float, v: Float) {
        count++
        red += r
        green += g
        blue += b
        saturation += s
        value += v
    }

    /** Favour frequent, saturated, reasonably bright colours. */
    fun score(): Float = if (count == 0) 0f else {
        val avgS = saturation / count
        val avgV = value / count
        count * (0.35f + avgS) * (0.35f + avgV)
    }

    fun color(): Color = Color(
        (red / count).toInt(),
        (green / count).toInt(),
        (blue / count).toInt()
    )
}

/** Pure sampler kept separate so it can be unit-tested with a tiny bitmap. */
fun dominantAccent(bitmap: Bitmap): Color? {
    if (bitmap.width <= 0 || bitmap.height <= 0) return null
    val stepX = max(1, bitmap.width / 48)
    val stepY = max(1, bitmap.height / 48)
    val hsv = FloatArray(3)
    val buckets = HashMap<Int, HueBucket>()

    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val argb = bitmap.getPixel(x, y)
            val alpha = android.graphics.Color.alpha(argb)
            if (alpha >= 200) {
                val r = android.graphics.Color.red(argb)
                val g = android.graphics.Color.green(argb)
                val b = android.graphics.Color.blue(argb)
                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                val hue = hsv[0]
                val saturation = hsv[1]
                val value = hsv[2]
                // Skip near-grey pixels: they carry no usable hue.
                if (saturation >= 0.18f && value >= 0.15f) {
                    val key = (hue / 15f).toInt()
                    val bucket = buckets.getOrPut(key) { HueBucket() }
                    bucket.add(r, g, b, saturation, value)
                }
            }
            x += stepX
        }
        y += stepY
    }

    val best = buckets.values.maxByOrNull { it.score() } ?: return null
    return ensureVivid(best.color())
}

/** Lifts dull artwork colours into an accent that stays readable as a fill. */
private fun ensureVivid(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = max(hsv[1], 0.55f)
    hsv[2] = hsv[2].coerceIn(0.55f, 0.86f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
