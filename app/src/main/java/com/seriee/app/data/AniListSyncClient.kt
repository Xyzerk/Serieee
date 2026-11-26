package com.seriee.app.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * F3: Two-way sync (outbound) to AniList. Pushes the local progress of a
 * title whose source is AniList back to the user's AniList list, using the
 * AniList GraphQL `SaveMediaListEntry` mutation.
 *
 * AniList has no personal API tokens: the user pastes an OAuth access token
 * (from the AniList OAuth flow) in API-key settings. Failures are silent
 * no-ops for callers that fire-and-forget; the token is only ever sent to
 * graphql.anilist.co.
 */
object AniListSyncClient {

    fun statusFor(status: WatchStatus): String = when (status) {
        WatchStatus.Watching -> "CURRENT"
        WatchStatus.Completed -> "COMPLETED"
        WatchStatus.PlanToWatch -> "PLANNING"
        WatchStatus.ComingSoon -> "PLANNING"
        WatchStatus.OnHold -> "PAUSED"
        WatchStatus.Dropped -> "DROPPED"
    }

    /** Returns true when AniList accepted the entry. */
    fun pushProgress(token: String, mediaId: Int, progress: Int, status: WatchStatus, score: Int): Boolean {
        if (token.isBlank() || mediaId <= 0) return false
        val body = JSONObject().apply {
            put(
                "query",
                """
                mutation (${'$'}mediaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}score: Float) {
                  SaveMediaListEntry(mediaId: ${'$'}mediaId, progress: ${'$'}progress, status: ${'$'}status, score: ${'$'}score) {
                    id progress status
                  }
                }
                """.trimIndent()
            )
            put(
                "variables", JSONObject()
                    .put("mediaId", mediaId)
                    .put("progress", progress.coerceAtLeast(0))
                    .put("status", statusFor(status))
                    .put("score", if (score in 1..5) score * 2.0 else 0.0)
            )
        }
        return runCatching {
            val conn = URL("https://graphql.anilist.co").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            conn.disconnect()
            code in 200..299 && !JSONObject(text).has("errors")
        }.getOrDefault(false)
    }
}
