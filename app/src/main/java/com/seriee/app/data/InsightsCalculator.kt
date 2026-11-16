package com.seriee.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/** Aggregated figures for the Insights tab. All fields are safe for an empty library. */
data class Insights(
    val totalTitles: Int = 0,
    val totalSeries: Int = 0,
    val totalMovies: Int = 0,
    val watchedEpisodes: Int = 0,
    val moviesWatched: Int = 0,
    val completedTitles: Int = 0,
    val completionRate: Int = 0,
    val averageRating: Float = 0f,
    val ratedTitles: Int = 0,
    val statusCounts: Map<WatchStatus, Int> = emptyMap(),
    /** Index 0 is always zero; indices 1..5 hold the star counts. */
    val ratingCounts: List<Int> = List(6) { 0 },
    val topTags: List<Pair<String, Int>> = emptyList(),
    val estimatedMinutes: Int = 0,
    val dailyCounts: Map<LocalDate, Int> = emptyMap(),
    val recent: List<WatchEvent> = emptyList()
)

/**
 * Pure calculator backing [Insights]. It reuses [MediaItem.watchedEpisodes]
 * instead of re-deriving progress, and buckets activity with the device's
 * current time zone.
 */
object InsightsCalculator {
    const val ACTIVITY_DAYS = 84L
    const val RECENT_LIMIT = 10
    const val MINUTES_PER_EPISODE = 24

    fun calculate(
        items: List<MediaItem>,
        history: List<WatchEvent>,
        watchDays: Set<String> = emptySet(),
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Insights {
        val series = items.count { it.type == MediaType.Series }
        val movies = items.count { it.type == MediaType.Movie }
        // Titles that have not started yet keep their default episode 1 (a
        // freshly fetched multi-season show often lands in Coming Soon while a
        // new season airs), so they must not look watched in the hero stats.
        val watchedEpisodes = items
            .filter { it.type == MediaType.Series && it.status.countsProgress() }
            .sumOf { it.watchedEpisodes }
        val moviesWatched = items.count {
            it.type == MediaType.Movie && it.status.countsProgress() && it.watchedEpisodes > 0
        }
        val completed = items.count { it.status == WatchStatus.Completed }
        val ratings = items.map { it.rating }.filter { it in 1..5 }

        val ratingCounts = MutableList(6) { 0 }
        ratings.forEach { ratingCounts[it]++ }

        return Insights(
            totalTitles = items.size,
            totalSeries = series,
            totalMovies = movies,
            watchedEpisodes = watchedEpisodes,
            moviesWatched = moviesWatched,
            completedTitles = completed,
            completionRate = if (items.isEmpty()) 0 else completed * 100 / items.size,
            averageRating = if (ratings.isEmpty()) 0f
            else (ratings.sum().toFloat() / ratings.size * 10f).roundToInt() / 10f,
            ratedTitles = ratings.size,
            statusCounts = WatchStatus.entries.associateWith { status ->
                items.count { it.status == status }
            },
            ratingCounts = ratingCounts,
            topTags = topTags(items),
            estimatedMinutes = watchedEpisodes * MINUTES_PER_EPISODE,
            dailyCounts = dailyCounts(history, watchDays, now, zone),
            recent = history.sortedByDescending { it.watchedAt }.take(RECENT_LIMIT)
        )
    }

    /**
     * Plan to Watch and Coming Soon have no viewing progress even when a
     * season counter was pre-filled by an import.
     */
    private fun WatchStatus.countsProgress(): Boolean =
        this != WatchStatus.PlanToWatch && this != WatchStatus.ComingSoon

    private fun topTags(items: List<MediaItem>): List<Pair<String, Int>> {
        // Key by lowercase tag, keep the first casing the user typed.
        val counts = LinkedHashMap<String, Pair<String, Int>>()
        items.forEach { item ->
            item.tags.forEach { raw ->
                val tag = raw.trim()
                if (tag.isEmpty()) return@forEach
                val key = tag.lowercase(Locale.ROOT)
                val current = counts[key]
                counts[key] = if (current == null) tag to 1 else current.first to current.second + 1
            }
        }
        return counts.values
            .sortedWith(
                compareByDescending<Pair<String, Int>> { it.second }
                    .thenBy { it.first.lowercase(Locale.ROOT) }
            )
            .take(8)
    }

    private fun dailyCounts(
        history: List<WatchEvent>,
        watchDays: Set<String>,
        now: Long,
        zone: ZoneId
    ): Map<LocalDate, Int> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val start = today.minusDays(ACTIVITY_DAYS - 1)
        val counts = mutableMapOf<LocalDate, Int>()

        history.forEach { event ->
            val date = Instant.ofEpochMilli(event.watchedAt).atZone(zone).toLocalDate()
            if (!date.isBefore(start) && !date.isAfter(today)) {
                counts[date] = (counts[date] ?: 0) + event.count.coerceAtLeast(1)
            }
        }

        // Pre-0.5 streak days have no event; seed them so the heatmap is not empty.
        watchDays.forEach { raw ->
            val date = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return@forEach
            if (!date.isBefore(start) && !date.isAfter(today) && counts[date] == null) {
                counts[date] = 1
            }
        }
        return counts
    }
}
