package com.seriee.app.data

import kotlin.math.abs

/**
 * Source-reported airing facts used for the derived **Ongoing** badge and the
 * stale-Coming-Soon repair. Mirrors the subset of [MetadataResult] that needs
 * to survive into storage and the UI.
 */
data class AiringInfo(
    val airing: Boolean,
    val airingStarted: Boolean,
    val nextEpisode: Int? = null,
    val nextEpisodeSeason: Int? = null,
    val nextAirAt: Long? = null,
)

/** Derived badge driven by airing data — never a [WatchStatus]. */
enum class AiringBadge { None, ComingSoon, Ongoing }

/**
 * Pure rule set for the Ongoing badge and the stale-[WatchStatus.ComingSoon]
 * repair (see `docs/specs/2026-11-21-airing-schedule-design.md`). No Android
 * dependencies: everything reads the persisted fields on the item.
 */
object AiringRules {

    fun isOngoing(item: MediaItem): Boolean = item.airing && item.airingStarted

    /**
     * Coming Soon needs *evidence* — `airing = true` or a future
     * [MediaItem.nextAirAt] from the source, or an already-started source with
     * no date recorded. A manual title with no metadata fields set must never
     * get it.
     */
    fun isComingSoon(item: MediaItem): Boolean =
        !item.airingStarted && (item.airing || item.nextAirAt != null)

    fun badge(item: MediaItem): AiringBadge = when {
        isOngoing(item) -> AiringBadge.Ongoing
        isComingSoon(item) -> AiringBadge.ComingSoon
        else -> AiringBadge.None
    }

    /** Copies the six [MediaItem]-suffixed aired fields and stamps the refresh time. */
    fun withAiring(item: MediaItem, info: AiringInfo, checkedAt: Long): MediaItem = item.copy(
        airing = info.airing,
        airingStarted = info.airingStarted,
        nextEpisode = info.nextEpisode,
        nextEpisodeSeason = info.nextEpisodeSeason,
        nextAirAt = info.nextAirAt,
        airingCheckedAt = checkedAt,
    )

    /**
     * Repairs a stale [WatchStatus.ComingSoon] that the refreshed source now
     * reports as **currently airing**: the stored date is missing, past, or
     * equal (within 60 s) to the next-episode airtime. Returns the repaired
     * copy or null when the item is not a stale Coming Soon.
     *
     * Deliberate non-repair cases: a Finished/Cancelled/Hiatus source reports
     * `airing = false`, and a user-set future date that differs from the next
     * airtime stays untouched.
     */
    fun repairComingSoon(item: MediaItem, now: Long): MediaItem? {
        if (item.status != WatchStatus.ComingSoon) return null
        if (!item.airing || item.airingCheckedAt <= 0) return null
        val releaseDate = item.releaseDate
        val stale = releaseDate == null ||
            releaseDate <= now ||
            (item.nextAirAt != null && abs(releaseDate!! - item.nextAirAt) <= 60_000)
        if (!stale) return null
        return item.copy(
            status = WatchStatus.Watching,
            releaseDate = null,
            reminderEnabled = false,
        )
    }
}
