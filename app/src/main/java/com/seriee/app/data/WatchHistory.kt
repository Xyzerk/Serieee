package com.seriee.app.data

/**
 * A compact log of explicit watch actions.
 *
 * [title] and [type] are snapshots so the timeline survives a rename or a
 * delete. When the item still exists the UI shows its current title; the
 * snapshot is only the fallback.
 */
data class WatchEvent(
    val itemId: String,
    val title: String,
    val type: MediaType,
    val season: Int,
    val episode: Int,
    /** Episodes covered by this action: 1 for a normal +, the delta for a jump. */
    val count: Int = 1,
    val watchedAt: Long
)

/** Caps and retention for [WatchEvent] storage. Pure so it is JVM-testable. */
object WatchHistoryRules {
    const val MAX_EVENTS = 500
    const val RETENTION_DAYS = 365

    fun trim(events: List<WatchEvent>, now: Long = System.currentTimeMillis()): List<WatchEvent> {
        val cutoff = now - RETENTION_DAYS * 86_400_000L
        return events
            .filter { it.watchedAt >= cutoff }
            .sortedByDescending { it.watchedAt }
            .distinctBy { it.itemId to (it.season to (it.episode to it.watchedAt)) }
            .take(MAX_EVENTS)
    }

    /** Appends an event and applies the usual trim/retention rules. */
    fun append(
        events: List<WatchEvent>,
        event: WatchEvent,
        now: Long = System.currentTimeMillis(),
    ): List<WatchEvent> = trim(events + event, now)
}
