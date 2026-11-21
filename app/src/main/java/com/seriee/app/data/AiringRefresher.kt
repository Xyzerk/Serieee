package com.seriee.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One "refresh one library's airing state" pass per app open.
 *
 * Selection:
 * - AniList: every item whose `sourceId` is numeric, chunked by 50.
 * - TVmaze: items where `airingCheckedAt == 0` (never checked), the stored
 *   next airtime is overdue, or the check is older than 12 h; at most 10,
 *   soonest `nextAirAt` first (nulls last).
 *
 * The result's updates are keyed by `MediaItem.id` and contain **only ids the
 * source answered with** — a failed or partial fetch is silent and retries
 * next open, so a missing key is not a signal to clear anything.
 */
internal class AiringRefresher(
    private val fetchAniList: suspend (List<String>) -> Map<String, AiringInfo>,
    private val fetchTvmaze: suspend (String) -> AiringInfo?,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private companion object {
        private const val ANILIST_CHUNK = 50
        private const val TVMAZE_STALE_MS = 12 * 60 * 60 * 1000L
        private const val TVMAZE_CAP = 10
    }

    suspend fun refresh(items: List<MediaItem>): AiringRefreshResult = withContext(Dispatchers.IO) {
        val checkedAt = now()
        val updates = LinkedHashMap<String, AiringInfo>()

        val aniListIds = items.asSequence()
            .filter { it.source.equals("AniList", ignoreCase = true) }
            .mapNotNull { it.sourceId.toIntOrNull()?.toString() }
            .distinct()
            .toList()
        aniListIds.chunked(ANILIST_CHUNK).forEach { chunk ->
            val byId = chunk.mapNotNull { id -> items.firstOrNull { it.sourceId == id }?.id to id }.toMap()
            fetchAniList(chunk).forEach { (id, info) ->
                byId[id]?.let { itemId -> updates[itemId] = info }
            }
        }

        val tvmaze = items.asSequence()
            .filter { it.source.equals("TVMaze", ignoreCase = true) && it.sourceId.isNotBlank() }
            .filter {
                it.airingCheckedAt == 0L ||
                    checkedAt - it.airingCheckedAt > TVMAZE_STALE_MS ||
                    (it.nextAirAt != null && it.nextAirAt <= checkedAt)
            }
            .sortedWith(compareBy({ it.nextAirAt ?: Long.MAX_VALUE }, { it.id }))
            .take(TVMAZE_CAP)
            .toList()
        tvmaze.forEach { item ->
            val info = fetchTvmaze(item.sourceId) ?: return@forEach
            updates[item.id] = info
        }

        AiringRefreshResult(updates, checkedAt)
    }
}

internal data class AiringRefreshResult(
    /** Source-reported updates keyed by the stored item's `MediaItem.id`. */
    val updates: Map<String, AiringInfo>,
    /** When the fetch pass ran; applied to the new airing fields. */
    val checkedAt: Long,
)

/**
 * Merges [AiringRefreshResult] onto a freshly-read list. Runs on the newest
 * data (edits during the refresh win); repairs only items that received an
 * update this run. `changed` is true when any item differs from the input.
 */
internal object AiringApply {
    data class Applied(
        val items: List<MediaItem>,
        val repairedIds: Set<String>,
        val changed: Boolean,
    )

    fun apply(items: List<MediaItem>, result: AiringRefreshResult): Applied {
        val merged = items.map { item ->
            val info = result.updates[item.id] ?: return@map item
            val updated = AiringRules.withAiring(item, info, result.checkedAt)
            AiringRules.repairComingSoon(updated, result.checkedAt) ?: updated
        }
        val repairedIds = items.mapIndexedNotNull { i, item ->
            if (merged[i] != item && result.updates[item.id] != null) item.id else null
        }.toSet()
        return Applied(merged, repairedIds, merged != items)
    }
}
