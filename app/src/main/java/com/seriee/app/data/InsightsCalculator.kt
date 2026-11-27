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
    val recent: List<WatchEvent> = emptyList(),
    /** Vetro P3b: finished vs dropped counts per genre (tags act as genres). */
    val genreOutcomes: List<GenreOutcome> = emptyList(),
    /** Vetro P3b: episodes logged in the last 90 days vs across all history. */
    val episodesLast90Days: Int = 0,
    val episodesAllTime: Int = 0,
    /** Vetro P3b: episodes per month for the last 6 months, oldest first ("yyyy-MM" to count). */
    val monthlyCounts: List<Pair<String, Int>> = emptyList()
)

/** Finished/dropped tallies for one genre (tag). */
data class GenreOutcome(
    val genre: String,
    val finished: Int,
    val dropped: Int
) {
    val total: Int get() = finished + dropped
}

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
        // Titles that have not started yet keep their default episode 0 (a
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
            estimatedMinutes = items
                .filter { it.type == MediaType.Series && it.status.countsProgress() }
                .sumOf { it.watchedEpisodes * (it.episodeDurationMin.takeIf { d -> d > 0 } ?: MINUTES_PER_EPISODE) },
            dailyCounts = dailyCounts(history, watchDays, now, zone),
            recent = history.sortedByDescending { it.watchedAt }.take(RECENT_LIMIT),
            genreOutcomes = genreOutcomes(items),
            episodesLast90Days = episodesSince(history, now - 90L * 86_400_000L),
            episodesAllTime = history.sumOf { it.count.coerceAtLeast(1) },
            monthlyCounts = monthlyCounts(history, now, zone)
        )
    }

    /** Finished (Completed) vs Dropped per genre; a title counts once per tag it carries. */
    private fun genreOutcomes(items: List<MediaItem>): List<GenreOutcome> {
        val byGenre = LinkedHashMap<String, GenreOutcome>()
        items.forEach { item ->
            if (item.status != WatchStatus.Completed && item.status != WatchStatus.Dropped) return@forEach
            item.tags.map { it.trim() }.filter { it.isNotEmpty() }
                .distinctBy { it.lowercase(Locale.ROOT) }
                .forEach { tag ->
                    val key = tag.lowercase(Locale.ROOT)
                    val cur = byGenre[key] ?: GenreOutcome(tag, 0, 0)
                    byGenre[key] = if (item.status == WatchStatus.Completed) {
                        cur.copy(genre = cur.genre, finished = cur.finished + 1)
                    } else {
                        cur.copy(dropped = cur.dropped + 1)
                    }
                }
        }
        return byGenre.values
            .sortedWith(compareByDescending<GenreOutcome> { it.total }.thenBy { it.genre.lowercase(Locale.ROOT) })
            .take(8)
    }

    private fun episodesSince(history: List<WatchEvent>, cutoff: Long): Int =
        history.filter { it.watchedAt >= cutoff }.sumOf { it.count.coerceAtLeast(1) }

    /** Episode counts bucketed by month for the 6 months ending in the current one. */
    private fun monthlyCounts(
        history: List<WatchEvent>,
        now: Long,
        zone: ZoneId
    ): List<Pair<String, Int>> {
        val current = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().withDayOfMonth(1)
        val months = (5 downTo 0).map { current.minusMonths(it.toLong()) }
        val counts = months.associate { month ->
            "%04d-%02d".format(month.year, month.monthValue) to 0
        }.toMutableMap()
        history.forEach { event ->
            val date = Instant.ofEpochMilli(event.watchedAt).atZone(zone).toLocalDate()
            val key = "%04d-%02d".format(date.year, date.monthValue)
            if (key in counts) counts[key] = (counts[key] ?: 0) + event.count.coerceAtLeast(1)
        }
        return months.map { month ->
            val key = "%04d-%02d".format(month.year, month.monthValue)
            key to (counts[key] ?: 0)
        }
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
