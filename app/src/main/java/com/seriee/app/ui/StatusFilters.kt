package com.seriee.app.ui

import com.seriee.app.R
import com.seriee.app.data.MediaItem
import com.seriee.app.data.WatchStatus

/**
 * Status filters shared by the Library and Progress screens.
 *
 * Selection is stored as a [WatchStatus] (`null` = "All") rather than a list
 * index, so reordering or adding an entry can never silently change a saved
 * selection, and the two screens cannot drift out of sync.
 */
internal val statusFilters: List<Pair<WatchStatus?, Int>> = listOf(
    null to R.string.filter_all,
    WatchStatus.Watching to R.string.filter_watching,
    WatchStatus.Completed to R.string.filter_completed,
    WatchStatus.PlanToWatch to R.string.filter_plan_to_watch,
    WatchStatus.ComingSoon to R.string.filter_coming_soon,
    WatchStatus.OnHold to R.string.filter_on_hold,
    WatchStatus.Dropped to R.string.filter_dropped
)

/** Number of titles matching [status]; `null` counts the whole library. */
internal fun List<MediaItem>.countWithStatus(status: WatchStatus?): Int =
    if (status == null) size else count { it.status == status }
