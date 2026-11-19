package com.seriee.app.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import com.seriee.app.R
import com.seriee.app.data.MediaItem

const val VLC_PACKAGE = "org.videolan.vlc"
const val MX_PACKAGE = "com.mxtech.videoplayer.ad"

fun openUrl(context: Context, url: String) {
    val fixed = if (url.contains("://")) url else "https://$url"
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixed)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.cannot_open_link), Toast.LENGTH_SHORT).show()
    }
}

/** Opens the picked offline video in an external player. [pkg] = null shows the system chooser. */
fun openVideo(context: Context, item: MediaItem, pkg: String?) {
    // Prefer per-episode file if available, else legacy single file
    val uriStr = item.effectiveSeasons
        .flatMap { it.episodes }
        .firstOrNull { it.number == item.currentEp }?.uri
        ?: item.videoUri
    if (uriStr.isBlank()) {
        Toast.makeText(
            context,
            context.getString(R.string.pick_file_first),
            Toast.LENGTH_LONG
        ).show()
        return
    }
    openVideoUri(context, uriStr, pkg)
}

fun openVideoUri(context: Context, uriString: String, pkg: String?) {
    if (uriString.isBlank()) {
        Toast.makeText(context, context.getString(R.string.pick_file_first), Toast.LENGTH_LONG).show()
        return
    }
    val uri = Uri.parse(uriString)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/*")
        clipData = ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (pkg != null) setPackage(pkg)
    }
    try {
        if (pkg == null) {
            val chooser = Intent.createChooser(intent, context.getString(R.string.open_with)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } else {
            context.startActivity(intent)
        }
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.player_not_installed), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.failed_open_video), Toast.LENGTH_SHORT).show()
    }
}

/**
 * Auto-detect (season, episode) from a filename. Null if not detectable.
 *
 * Understands `S01E02`, `E05` and `Episode 5`, then falls back to the last
 * "safe" number so release-group names like `CHFY--01.mkv` or
 * `[Group] Show Title - 07 (1080p) [ABCD1234].mkv` still resolve. Numbers that
 * are resolutions, codecs, years or part of a `5.1` audio tag are skipped.
 */
object EpisodeFileParser {
    private const val MAX_EPISODE = 999

    private val sxe = Regex("""S(\d+)[^E0-9]*E(\d+)""", RegexOption.IGNORE_CASE)
    private val explicitEpisode = Regex("""(?:^|[^A-Za-z])E(?:P|PISODE)?\s*0*(\d+)""", RegexOption.IGNORE_CASE)
    private val videoExtension =
        Regex("""\.(?:mkv|mp4|avi|mov|m4v|webm|ts|m2ts|wmv|flv|mpg|mpeg|3gp|ogv)$""", RegexOption.IGNORE_CASE)
    private val bracketGroup = Regex("""\[[^\]]*]|\([^)]*\)""")
    private val versionSuffix = Regex("""(?<=\d)[vV]\d{1,2}\b""")
    private val separators = Regex("""[._\-+~|]+""")
    private val resolutionTokens = setOf(360, 480, 540, 576, 720, 1080, 1440, 2160, 4320)
    private val codecTokens = setOf(264, 265)

    fun parseSeasonEpisode(filename: String): Pair<Int, Int>? {
        sxe.find(filename)?.let { m ->
            val s = m.groupValues[1].toIntOrNull() ?: 1
            val e = m.groupValues[2].toIntOrNull() ?: return@let null
            return s to e
        }
        return null
    }

    fun parseEpisode(filename: String): Int? = parseEpisode(filename, emptyList())

    /**
     * @param titles known names for the show ([MediaItem.title] plus its hidden
     *   alternate titles); a matched title is stripped before scanning, so a
     *   year or number inside the title is not mistaken for the episode.
     * @param maxEpisodes when known, numeric candidates above it are ignored.
     */
    fun parseEpisode(
        filename: String,
        titles: List<String>,
        maxEpisodes: Int = MAX_EPISODE
    ): Int? {
        explicitEpisode.find(filename)?.let { return it.groupValues[1].toIntOrNull() }
        val tokens = tokenize(filename)
        val withoutTitle = tokens.withoutAnyOf(titles.map { tokenize(it) }.filter { it.isNotEmpty() })
        return safeNumber(withoutTitle, maxEpisodes) ?: safeNumber(tokens, maxEpisodes)
    }

    private fun tokenize(filename: String): List<String> =
        filename.substringAfterLast('/').substringAfterLast('\\')
            .replace(videoExtension, "")
            .replace(versionSuffix, "")
            .replace(bracketGroup, " ")
            .replace(separators, " ")
            .split(' ')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

    /** Removes the longest contiguous run of tokens matching a known title. */
    private fun List<String>.withoutAnyOf(titles: List<List<String>>): List<String> {
        val match = titles.filter { it.size <= size }.maxByOrNull { it.size } ?: return this
        for (start in 0..(size - match.size)) {
            if (subList(start, start + match.size) == match) {
                return take(start) + drop(start + match.size)
            }
        }
        return this
    }

    private fun safeNumber(tokens: List<String>, maxEpisodes: Int): Int? {
        val candidates = mutableListOf<Int>()
        var i = 0
        while (i < tokens.size) {
            // "5.1" / "2.0" audio layout tokenises to two numbers.
            if (i + 1 < tokens.size && tokens[i] == "5" && tokens[i + 1] == "1") {
                i += 2
                continue
            }
            if (i + 1 < tokens.size && tokens[i] == "2" && tokens[i + 1] == "0") {
                i += 2
                continue
            }
            val token = tokens[i]
            val value = token.toIntOrNull()
            if (value != null && token.length <= 4 && value > 0 &&
                value !in resolutionTokens && value !in codecTokens &&
                value !in 1900..2100 && value <= maxEpisodes
            ) {
                candidates.add(value)
            }
            i++
        }
        // Fansub / release names put the episode number last, so use the last
        // safe candidate (e.g. `CHFY--01`, `Show 2023 - 12`).
        return candidates.lastOrNull()
    }
}

/** Case-insensitive natural ordering, so `ep 2` sorts before `ep 10`. */
fun naturalFileOrder(a: String, b: String): Int {
    val chunksA = Regex("""\d+|\D+""").findAll(a.lowercase()).map { it.value }.toList()
    val chunksB = Regex("""\d+|\D+""").findAll(b.lowercase()).map { it.value }.toList()
    for (i in 0 until minOf(chunksA.size, chunksB.size)) {
        val x = chunksA[i]
        val y = chunksB[i]
        val xn = x.toLongOrNull()
        val yn = y.toLongOrNull()
        val cmp = if (xn != null && yn != null) xn.compareTo(yn) else x.compareTo(y)
        if (cmp != 0) return cmp
    }
    return chunksA.size.compareTo(chunksB.size)
}

fun displayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }
}

fun relativeTime(context: Context, ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> context.getString(R.string.time_just_now)
        minutes < 60 -> context.getString(R.string.time_minutes_ago, minutes)
        hours < 24 -> context.getString(R.string.time_hours_ago, hours)
        days < 2 -> context.getString(R.string.time_yesterday)
        days < 7 -> context.getString(R.string.time_days_ago, days)
        days < 30 -> context.getString(R.string.time_weeks_ago, days / 7)
        else -> context.getString(R.string.time_months_ago, days / 30)
    }
}
