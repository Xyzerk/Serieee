package com.seriee.app.data

import android.content.Context
import android.net.Uri
import com.seriee.app.debug.AppLogger
import java.io.File

object PosterStore {
    private const val DEFAULT_EXTENSION = "jpg"
    private val ALLOWED_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif"
    )

    fun copyPosterToInternal(context: Context, uri: Uri, mediaId: String): String? {
        return try {
            val resolver = context.contentResolver
            val ext = sanitizeExtension(
                resolver.getType(uri)?.substringAfterLast('/')
                    ?: uri.lastPathSegment?.substringAfterLast('.', "")
            )
            val dir = File(context.filesDir, "posters")
            if (!dir.exists()) dir.mkdirs()
            val safeId = mediaId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
                .take(64)
                .ifEmpty { "poster" }
            val dest = File(dir, "${safeId}_${System.currentTimeMillis()}.$ext")
            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            "posters/${dest.name}"
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Restricts the file extension to known image types. MIME types and path
     * segments come from the picked document's provider, so they must not be
     * trusted as a file-name suffix.
     */
    internal fun sanitizeExtension(raw: String?): String {
        val ext = raw?.trim()?.lowercase()?.removePrefix(".").orEmpty()
        return if (ext in ALLOWED_EXTENSIONS) ext else DEFAULT_EXTENSION
    }

    fun fileFor(context: Context, relativePath: String): File? {
        if (relativePath.startsWith("posters/")) {
            val f = File(context.filesDir, relativePath)
            return if (f.exists()) f else null
        }
        return null
    }

    fun deleteIfLocal(context: Context, poster: String) {
        if (poster.startsWith("posters/")) {
            try {
                val f = File(context.filesDir, poster)
                if (f.exists()) f.delete()
            } catch (_: Exception) { }
        }
    }

    fun isLocalPath(poster: String): Boolean = poster.startsWith("posters/")
    fun isRemoteUrl(poster: String): Boolean = poster.startsWith("http://") || poster.startsWith("https://")

    /**
     * Deletes cached poster files that no longer belong to a library item.
     * [activePosters] holds relative paths such as "posters/&lt;id&gt;_&lt;ts&gt;.jpg".
     */
    fun pruneUnused(context: Context, activePosters: Set<String>) {
        runCatching {
            val dir = File(context.filesDir, "posters")
            if (!dir.exists() || !dir.isDirectory) return
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.isFile && "posters/${f.name}" !in activePosters) {
                    f.delete()
                }
            }
        }.onFailure { err ->
            AppLogger.w("PosterStore", "Failed to prune unused posters", err)
        }
    }
}
