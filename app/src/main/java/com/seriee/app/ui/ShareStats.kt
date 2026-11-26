package com.seriee.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.content.FileProvider
import com.seriee.app.data.Insights
import java.io.File
import java.io.FileOutputStream

/**
 * F8: Render the Insights summary as a shareable 1080x1350 stats card image
 * and open the system share sheet with it.
 */
fun shareStatsCard(context: Context, insights: Insights) {
    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bg = Paint().apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            Color.rgb(24, 24, 38), Color.rgb(58, 36, 92), Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

    val title = Paint().apply {
        color = Color.WHITE
        textSize = 84f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val label = Paint().apply {
        color = Color.rgb(190, 190, 210)
        textSize = 44f
        isAntiAlias = true
    }
    val value = Paint().apply {
        color = Color.WHITE
        textSize = 64f
        isFakeBoldText = true
        isAntiAlias = true
    }

    canvas.drawText("Seriee", 72f, 150f, title)
    canvas.drawText("Statistik Tontonan Saya", 72f, 225f, label)

    val hours = insights.estimatedMinutes / 60
    val rows = listOf(
        "Judul di library" to insights.totalTitles.toString(),
        "Episode ditonton" to insights.watchedEpisodes.toString(),
        "Film selesai" to insights.moviesWatched.toString(),
        "Perkiraan waktu" to "$hours jam",
        "Judul tamat" to insights.completedTitles.toString(),
        "Tingkat penyelesaian" to "${insights.completionRate}%",
        "Rating rata-rata" to if (insights.ratedTitles > 0) {
            String.format(java.util.Locale.US, "%.1f / 5", insights.averageRating)
        } else "-"
    )
    var y = 380f
    rows.forEach { (k, v) ->
        canvas.drawText(k, 72f, y, label)
        canvas.drawText(v, 72f, y + 78f, value)
        y += 168f
    }

    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "seriee-stats.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Statistik tontonan saya dari Seriee")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan statistik"))
}
