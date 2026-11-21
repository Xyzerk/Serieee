package com.seriee.app.ui

import android.content.Context
import com.seriee.app.R
import com.seriee.app.data.AiringRules
import com.seriee.app.data.MediaItem
import com.seriee.app.data.MetadataResult
import com.seriee.app.data.WatchStatus
import com.seriee.app.reminder.ReminderScheduler

/**
 * Shared suffix text for the metadata search results, cards, widget rows,
 * and the detail sheet. Returns null when there is nothing to show.
 */
internal data class AiringSuffix(val episode: Int?, val airAt: Long)

/**
 * Pure decision: does this item have something on screen worth showing?
 * - [AiringSuffix.episode] is non-null for Ongoing items with a known next
 *   episode; Coming Soon shows the date only (the countdown lives on).
 * - Null once [MediaItem.nextAirAt] has passed or is unknown.
 */
internal fun airingSuffixFor(item: MediaItem, now: Long): AiringSuffix? {
    val ongoing = AiringRules.isOngoing(item)
    val comingSoon = AiringRules.isComingSoon(item)
    if (!ongoing && !comingSoon) return null
    val at = item.nextAirAt ?: return null
    if (at <= now) return null
    return AiringSuffix(episode = if (ongoing) item.nextEpisode else null, airAt = at)
}

/** The suffix for a search-row [MetadataResult] (not yet persisted). */
internal fun metAiringSuffix(result: MetadataResult, now: Long): AiringSuffix? {
    val at = if (result.upcoming) result.releaseAt ?: result.nextAirAt else result.nextAirAt
    if (at == null || at <= now) return null
    return AiringSuffix(episode = if (!result.upcoming && result.airingStarted) result.nextEpisode else null, airAt = at)
}

internal fun formatAiringSuffix(context: Context, suffix: AiringSuffix): String =
    if (suffix.episode != null) {
        context.getString(R.string.airing_episode_short, suffix.episode, ReminderScheduler.formatShortDateSuffix(context, suffix.airAt))
    } else {
        ReminderScheduler.formatShortDateSuffix(context, suffix.airAt)
    }

/** The card/widget suffix ("Ep 9 · 27 Nov 05:00" or "27 Nov 05:00"); null hides it. */
internal fun airingSuffix(context: Context, item: MediaItem, now: Long): String? =
    airingSuffixFor(item, now)?.let { formatAiringSuffix(context, it) }

/** The detail-sheet line; null when the item has nothing to say. */
internal fun airingFullLabel(context: Context, item: MediaItem, now: Long): String? = when {
    AiringRules.isOngoing(item) && item.nextEpisode != null && item.nextAirAt != null && item.nextAirAt > now ->
        context.getString(R.string.airing_ongoing_full, item.nextEpisode, ReminderScheduler.formatShortDateTime(context, item.nextAirAt))
    AiringRules.isOngoing(item) -> context.getString(R.string.airing_ongoing_tba)
    AiringRules.isComingSoon(item) && item.nextAirAt != null && item.nextAirAt > now ->
        context.getString(R.string.airing_coming_soon_full, ReminderScheduler.formatShortDateTime(context, item.nextAirAt))
    AiringRules.isComingSoon(item) -> context.getString(R.string.status_coming_soon)
    else -> null
}

/** Short label for the 68 dp Progress poster: `Ep 9`, or `Ongoing` when TBA. */
fun progressBadgeLabel(context: Context, item: MediaItem, now: Long): String? {
    val suffix = airingSuffixFor(item, now) ?: return null
    return if (suffix.episode != null) {
        context.getString(R.string.airing_episode_only, suffix.episode)
    } else {
        context.getString(R.string.filter_ongoing)
    }
}

/** Full `Ep 9 · 27 Nov 21:00` label for the larger Library grid poster. */
fun libraryBadgeLabel(context: Context, item: MediaItem, now: Long): String? =
    airingSuffixFor(item, now)?.let { formatAiringSuffix(context, it) }

/** The bottom countdown strip belongs to premieres, never to airing episodes. */
fun showCountdownStrip(item: MediaItem): Boolean = item.status == WatchStatus.ComingSoon
