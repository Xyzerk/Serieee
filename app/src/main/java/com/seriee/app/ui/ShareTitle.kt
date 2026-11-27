package com.seriee.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import com.seriee.app.data.MediaItem
import com.seriee.app.data.MediaType
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Vetro P1a: render ONE title as a frosted-glass share card (1080x1350 PNG)
 * and open the system share sheet with it.
 *
 * Full mode shows nickname, poster, progress, rating and the title's
 * thought-bubble comment ([MediaItem.shareComment]); minimal mode keeps only
 * the poster, title and progress.
 */
suspend fun shareTitleCard(
    context: Context,
    item: MediaItem,
    profileName: String,
    full: Boolean
) {
    val poster = loadPosterBitmap(context, item.poster)
    val bitmap = renderShareTitleCard(item, profileName, poster, full)
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "seriee-title-${item.id.hashCode()}.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, item.title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan kartu"))
}

/** Best-effort poster decode: Coil for remote/local URIs, direct file decode as fallback. */
private suspend fun loadPosterBitmap(context: Context, poster: String): Bitmap? {
    if (poster.isBlank()) return null
    val fromFile = runCatching {
        val f = File(poster)
        if (f.isFile) BitmapFactory.decodeFile(f.absolutePath) else null
    }.getOrNull()
    if (fromFile != null) return fromFile
    return runCatching {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(poster)
            .allowHardware(false)
            .build()
        (loader.execute(request).drawable as? BitmapDrawable)?.bitmap
    }.getOrNull()
}

fun renderShareTitleCard(
    item: MediaItem,
    profileName: String,
    poster: Bitmap?,
    full: Boolean
): Bitmap {
    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Backdrop: deep diagonal gradient (same family as the stats card).
    val bg = Paint().apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            Color.rgb(24, 24, 38), Color.rgb(58, 36, 92), Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

    // Frosted-glass panel: translucent white with a bright hairline border.
    val panel = RectF(56f, 56f, width - 56f, height - 56f)
    val glassFill = Paint().apply {
        color = Color.argb(38, 255, 255, 255)
        isAntiAlias = true
    }
    canvas.drawRoundRect(panel, 48f, 48f, glassFill)
    val glassBorder = Paint().apply {
        color = Color.argb(110, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    canvas.drawRoundRect(panel, 48f, 48f, glassBorder)

    val namePaint = TextPaint().apply {
        color = Color.rgb(205, 205, 225)
        textSize = 40f
        isAntiAlias = true
    }
    val titlePaint = TextPaint().apply {
        color = Color.WHITE
        textSize = 68f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val bodyPaint = TextPaint().apply {
        color = Color.rgb(225, 225, 240)
        textSize = 44f
        isAntiAlias = true
    }
    val mutedPaint = TextPaint().apply {
        color = Color.rgb(175, 175, 200)
        textSize = 38f
        isAntiAlias = true
    }

    var y = 120f
    if (full) {
        val who = profileName.trim().ifBlank { "Seriee" }
        canvas.drawText(who, 104f, y + 40f, namePaint)
        y += 84f
    }

    // Poster (centre-cropped into a rounded slot) or a colour block with the initial.
    val posterRect = RectF(104f, y, width - 104f, y + 560f)
    if (poster != null) {
        // Centre-crop the poster into the rounded slot.
        val scale = maxOf(
            posterRect.width() / poster.width,
            posterRect.height() / poster.height
        )
        val dw = poster.width * scale
        val dh = poster.height * scale
        val dst = RectF(
            posterRect.left + (posterRect.width() - dw) / 2f,
            posterRect.top + (posterRect.height() - dh) / 2f,
            posterRect.left + (posterRect.width() - dw) / 2f + dw,
            posterRect.top + (posterRect.height() - dh) / 2f + dh
        )
        val save = canvas.save()
        val path = android.graphics.Path().apply {
            addRoundRect(posterRect, 32f, 32f, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(path)
        canvas.drawBitmap(poster, null, dst, Paint().apply { isFilterBitmap = true })
        canvas.restoreToCount(save)
    } else {
        val block = Paint().apply {
            color = Color.rgb(84, 62, 128)
            isAntiAlias = true
        }
        canvas.drawRoundRect(posterRect, 32f, 32f, block)
        val initialPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 260f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            item.title.trim().take(1).uppercase().ifBlank { "?" },
            posterRect.centerX(),
            posterRect.centerY() + 92f,
            initialPaint
        )
    }
    y = posterRect.bottom + 96f

    // Title (wrapped, max 2 lines).
    val titleLayout = StaticLayout.Builder
        .obtain(item.title, 0, item.title.length, titlePaint, width - 208)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setMaxLines(2)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()
    canvas.save()
    canvas.translate(104f, y)
    titleLayout.draw(canvas)
    canvas.restore()
    y += titleLayout.height + 28f

    // Progress line + bar.
    val progressText = if (item.type == MediaType.Movie) {
        if (item.watchedEpisodes > 0) "Sudah ditonton" else "Belum ditonton"
    } else {
        val activeSeason = item.effectiveSeasons.firstOrNull { it.currentEp < it.totalEp }
            ?: item.effectiveSeasons.lastOrNull()
        val seasonLabel = activeSeason?.let { "S${it.number} " } ?: ""
        "${seasonLabel}Episode ${item.watchedEpisodes}/${item.totalEpisodes} • ${item.percent}%"
    }
    canvas.drawText(progressText, 104f, y + 40f, bodyPaint)
    y += 66f
    val barBg = Paint().apply { color = Color.argb(60, 255, 255, 255); isAntiAlias = true }
    val barRect = RectF(104f, y, width - 104f, y + 18f)
    canvas.drawRoundRect(barRect, 9f, 9f, barBg)
    if (item.fraction > 0f) {
        val barFill = Paint().apply { color = Color.rgb(167, 139, 250); isAntiAlias = true }
        val fillRect = RectF(
            barRect.left, barRect.top,
            barRect.left + barRect.width() * item.fraction.coerceIn(0f, 1f),
            barRect.bottom
        )
        canvas.drawRoundRect(fillRect, 9f, 9f, barFill)
    }
    y += 64f

    if (full && item.rating > 0) {
        val stars = "★".repeat(item.rating) + "☆".repeat(5 - item.rating)
        canvas.drawText("$stars  ${item.rating}/5", 104f, y + 40f, bodyPaint)
        y += 84f
    }

    // Thought bubble with the saved comment.
    if (full && item.shareComment.isNotBlank()) {
        val comment = item.shareComment.take(52)
        val bubblePaint = Paint().apply {
            color = Color.argb(235, 255, 255, 255)
            isAntiAlias = true
        }
        val commentPaint = TextPaint().apply {
            color = Color.rgb(38, 32, 54)
            textSize = 42f
            isAntiAlias = true
        }
        val commentLayout = StaticLayout.Builder
            .obtain(comment, 0, comment.length, commentPaint, width - 208 - 64)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(3)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        val bubbleHeight = commentLayout.height + 56f
        val bubble = RectF(104f, y, width - 104f, min(y + bubbleHeight, height - 120f))
        canvas.drawRoundRect(bubble, 36f, 36f, bubblePaint)
        // Little tail pointing down-left.
        val tail = android.graphics.Path().apply {
            moveTo(bubble.left + 72f, bubble.bottom - 2f)
            lineTo(bubble.left + 118f, bubble.bottom - 2f)
            lineTo(bubble.left + 78f, bubble.bottom + 34f)
            close()
        }
        canvas.drawPath(tail, bubblePaint)
        canvas.save()
        canvas.translate(bubble.left + 32f, bubble.top + 28f)
        commentLayout.draw(canvas)
        canvas.restore()
    }

    return bitmap
}
