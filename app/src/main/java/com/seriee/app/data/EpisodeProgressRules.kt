package com.seriee.app.data

/** Result of advancing an item by one episode, including the episode that was logged. */
data class EpisodeIncrement(
    val item: MediaItem,
    val season: Int,
    val episode: Int,
)

/**
 * Single source of truth for "watch one more episode" behaviour.
 *
 * Both the in-app `+` button and the home-screen widget use this so progress,
 * completion state and the legacy (seasons-less) data shape stay identical.
 */
object EpisodeProgressRules {

    /** True when [item] has at least one episode left to watch. */
    fun canIncrement(item: MediaItem): Boolean = when {
        item.type == MediaType.Movie -> item.currentEp < 1
        item.seasons.isNotEmpty() -> item.seasons.any { it.currentEp < it.totalEp }
        else -> item.currentEp < item.totalEp
    }

    /**
     * Returns the updated item plus the season/episode that was watched, or null
     * when there is nothing left to advance.
     */
    fun increment(item: MediaItem, now: Long = System.currentTimeMillis()): EpisodeIncrement? {
        if (item.type == MediaType.Movie) {
            if (item.currentEp >= 1) return null
            return EpisodeIncrement(
                item = item.copy(
                    currentEp = 1,
                    totalEp = 1,
                    status = WatchStatus.Completed,
                    updatedAt = now,
                ),
                season = item.season,
                episode = 1,
            )
        }

        if (item.seasons.isNotEmpty()) {
            val index = item.seasons.indexOfFirst { it.currentEp < it.totalEp }
            if (index < 0) return null
            val newSeasons = item.seasons.mapIndexed { i, season ->
                if (i == index) season.copy(currentEp = (season.currentEp + 1).coerceAtMost(season.totalEp)) else season
            }
            val watched = newSeasons.sumOf { it.currentEp }
            val total = newSeasons.sumOf { it.totalEp }
            val activeSeason = newSeasons.firstOrNull { it.currentEp < it.totalEp } ?: newSeasons.last()
            val incremented = newSeasons[index]
            return EpisodeIncrement(
                item = item.copy(
                    seasons = newSeasons,
                    season = activeSeason.number,
                    currentEp = watched,
                    totalEp = total.coerceAtLeast(1),
                    status = if (total > 0 && watched >= total) WatchStatus.Completed else item.status,
                    updatedAt = now,
                ),
                season = incremented.number,
                episode = incremented.currentEp,
            )
        }

        if (item.currentEp >= item.totalEp) return null
        val next = item.currentEp + 1
        return EpisodeIncrement(
            item = item.copy(
                currentEp = next,
                status = if (next >= item.totalEp) WatchStatus.Completed else item.status,
                updatedAt = now,
            ),
            season = item.season,
            episode = next,
        )
    }
}
