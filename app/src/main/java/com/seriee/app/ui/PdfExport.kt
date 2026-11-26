package com.seriee.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.seriee.app.data.MediaItem
import java.io.File
import java.io.FileOutputStream

/**
 * Vetro P3d: export the whole collection as a simple printable PDF list —
 * one line per title with its status, progress and rating. Written either to
 * a user-picked document ([writeCollectionPdf]) or to the cache for sharing
 * ([shareCollectionPdf]).
 */
private const val PAGE_WIDTH = 595 // A4 at 72 dpi
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val LINE_HEIGHT = 22f

private fun buildCollectionPdf(items: List<MediaItem>): PdfDocument {
    val document = PdfDocument()
    val titlePaint = Paint().apply {
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val bodyPaint = Paint().apply {
        textSize = 11.5f
        isAntiAlias = true
    }
    val mutedPaint = Paint().apply {
        textSize = 10f
        color = android.graphics.Color.rgb(110, 110, 110)
        isAntiAlias = true
    }

    val sorted = items.sortedWith(
        compareBy<MediaItem> { it.status.ordinal }.thenBy { it.title.lowercase() }
    )
    var pageNumber = 1
    var page = document.startPage(
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
    )
    var canvas = page.canvas
    var y = MARGIN + 10f

    fun newPage() {
        document.finishPage(page)
        pageNumber++
        page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )
        canvas = page.canvas
        y = MARGIN
    }

    canvas.drawText("Seriee — Koleksi Saya", MARGIN, y, titlePaint)
    y += LINE_HEIGHT
    canvas.drawText("${items.size} judul", MARGIN, y, mutedPaint)
    y += LINE_HEIGHT + 6f

    sorted.forEach { item ->
        if (y > PAGE_HEIGHT - MARGIN) newPage()
        val progress = if (item.type == com.seriee.app.data.MediaType.Movie) {
            if (item.watchedEpisodes > 0) "Ditonton" else "Belum ditonton"
        } else {
            "${item.watchedEpisodes}/${item.totalEpisodes} ep (${item.percent}%)"
        }
        val rating = if (item.rating > 0) "★ ${item.rating}/5" else "★ -"
        val line = "${item.title} — ${item.status.name} • $progress • $rating"
        // Truncate defensively so a long title never runs off the page.
        val maxChars = 95
        canvas.drawText(line.take(maxChars), MARGIN, y, bodyPaint)
        y += LINE_HEIGHT
    }

    document.finishPage(page)
    return document
}

/** Writes the collection PDF into [uri] (e.g. from a CreateDocument picker). */
fun writeCollectionPdf(context: Context, uri: Uri, items: List<MediaItem>): Result<Int> =
    runCatching {
        val document = buildCollectionPdf(items)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            document.writeTo(out)
        } ?: error("Could not open output stream")
        document.close()
        items.size
    }

/** Renders the collection PDF into the cache and opens the share sheet. */
fun shareCollectionPdf(context: Context, items: List<MediaItem>) {
    val document = buildCollectionPdf(items)
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "seriee-collection.pdf")
    FileOutputStream(file).use { out -> document.writeTo(out) }
    document.close()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Koleksi Seriee saya")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan PDF"))
}
