package com.seriee.app.ui

import androidx.annotation.StringRes
import com.seriee.app.R
import com.seriee.app.data.AiringRules
import com.seriee.app.data.MediaItem
import com.seriee.app.data.WatchStatus

/**
 * Filters shared by the Library and Progress screens. The selection is
 * stored as a [LibraryFilter] (enum, so it saves by name) rather than a
 * [WatchStatus] or list index, so reordering or adding an entry can never
 * silently change a saved selection and the two screens cannot drift.
 *
 * `Ongoing` is derived (automatic) from airing data — it is not a status —
 * so a show may match both `Watching` and `Ongoing` when it is still airing.
 */
internal enum class LibraryFilter(@StringRes val labelRes: Int) {
    All(R.string.filter_all),
    Watching(R.string.filter_watching),
    Completed(R.string.filter_completed),
    PlanToWatch(R.string.filter_plan_to_watch),
    Ongoing(R.string.filter_ongoing),
    ComingSoon(R.string.filter_coming_soon),
    OnHold(R.string.filter_on_hold),
    Dropped(R.string.filter_dropped),
}

/** `status` of the filter: `null` for the two derived filters (`All`, `Ongoing`). */
internal val LibraryFilter.status: WatchStatus?
    get() = when (this) {
        LibraryFilter.All, LibraryFilter.Ongoing -> null
        LibraryFilter.Watching -> WatchStatus.Watching
        LibraryFilter.Completed -> WatchStatus.Completed
        LibraryFilter.PlanToWatch -> WatchStatus.PlanToWatch
        LibraryFilter.ComingSoon -> WatchStatus.ComingSoon
        LibraryFilter.OnHold -> WatchStatus.OnHold
        LibraryFilter.Dropped -> WatchStatus.Dropped
    }

internal val libraryFilters: List<LibraryFilter> = LibraryFilter.entries

internal fun LibraryFilter.matches(item: MediaItem): Boolean = when (this) {
    LibraryFilter.All -> true
    LibraryFilter.Ongoing -> AiringRules.isOngoing(item)
    else -> item.status == status
}

/** Number of titles matching [filter]; `All` counts the whole library. */
internal fun List<MediaItem>.countMatching(filter: LibraryFilter): Int = count { filter.matches(it) }

/** Number of titles matching a status (kept for callers that used it). */
internal fun List<MediaItem>.countWithStatus(status: WatchStatus?): Int =
    if (status == null) size else count { it.status == status }
