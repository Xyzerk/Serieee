package com.seriee.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.seriee.app.MainActivity
import com.seriee.app.R
import com.seriee.app.data.MediaItem
import com.seriee.app.data.MediaType
import com.seriee.app.data.SerieeRepository
import com.seriee.app.data.WatchEvent
import com.seriee.app.data.WatchHistoryRules
import com.seriee.app.data.WatchStatus
import com.seriee.app.debug.AppLogger

class SerieeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        const val ACTION_INCREMENT = "com.seriee.app.widget.ACTION_INCREMENT"
        const val EXTRA_ITEM_ID = "extra_item_id"

        fun notifyChanged(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, SerieeWidgetProvider::class.java))
            if (ids.isEmpty()) return
            for (id in ids) {
                updateWidget(context, mgr, id)
            }
        }

        /**
         * Applies a single +1 from the widget. Called by [WidgetIncrementReceiver],
         * which is not exported, so no other app can change watch progress.
         */
        fun incrementFromWidget(context: Context, itemId: String) {
            runCatching {
                val repo = SerieeRepository(context)
                val items = repo.loadItems()
                val target = items.firstOrNull { it.id == itemId } ?: return
                // Only active, released titles can advance from the widget.
                if (target.status != WatchStatus.Watching) return
                val now = System.currentTimeMillis()
                if (target.type == MediaType.Movie) {
                    if (target.currentEp >= 1) return
                    val updated = target.copy(
                        currentEp = 1,
                        totalEp = 1,
                        status = WatchStatus.Completed,
                        updatedAt = now,
                    )
                    repo.saveItems(items.map { if (it.id == itemId) updated else it })
                    repo.recordWatchToday()
                    repo.saveHistory(
                        WatchHistoryRules.append(
                            repo.loadHistory(),
                            WatchEvent(updated.id, updated.title, updated.type, 1, 1, 1, now),
                            now,
                        )
                    )
                    return
                }
                val seasons = target.effectiveSeasons.toMutableList()
                var incSeason = seasons.firstOrNull()?.number ?: 1
                var incEp = 1
                var bumped = false
                for (i in seasons.indices) {
                    val sec = seasons[i]
                    if (sec.currentEp < sec.totalEp) {
                        val nextEp = sec.currentEp + 1
                        seasons[i] = sec.copy(currentEp = nextEp)
                        incSeason = sec.number
                        incEp = nextEp
                        bumped = true
                        break
                    }
                }
                if (!bumped) return
                val allDone = seasons.all { it.currentEp >= it.totalEp }
                val updated = target.copy(
                    seasons = seasons,
                    season = incSeason,
                    currentEp = seasons.sumOf { it.currentEp },
                    totalEp = seasons.sumOf { it.totalEp.coerceAtLeast(1) }.coerceAtLeast(1),
                    status = if (allDone) WatchStatus.Completed else WatchStatus.Watching,
                    updatedAt = now,
                )
                repo.saveItems(items.map { if (it.id == itemId) updated else it })
                repo.recordWatchToday()
                repo.saveHistory(
                    WatchHistoryRules.append(
                        repo.loadHistory(),
                        WatchEvent(updated.id, updated.title, updated.type, incSeason, incEp, 1, now),
                        now,
                    )
                )
            }.onFailure { err ->
                AppLogger.w("Widget", "Failed to increment episode from widget", err)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_seriee)
            val openIntent = Intent(context, MainActivity::class.java)
            val openPi = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_header, openPi)

            val items = SerieeRepository(context).loadItems()
            val active = items
                .filter { it.status == WatchStatus.Watching }
                .sortedWith(
                    compareByDescending<MediaItem> { it.pinned }
                        .thenByDescending { it.updatedAt }
                )
                .take(3)

            val rowIds = intArrayOf(R.id.widget_row_0, R.id.widget_row_1, R.id.widget_row_2)
            val titleIds = intArrayOf(R.id.widget_item_title_0, R.id.widget_item_title_1, R.id.widget_item_title_2)
            val subIds = intArrayOf(R.id.widget_item_sub_0, R.id.widget_item_sub_1, R.id.widget_item_sub_2)
            val btnIds = intArrayOf(R.id.widget_item_inc_0, R.id.widget_item_inc_1, R.id.widget_item_inc_2)

            if (active.isEmpty()) {
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.widget_empty, openPi)
                for (rId in rowIds) views.setViewVisibility(rId, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_empty, View.GONE)
                for (i in rowIds.indices) {
                    if (i < active.size) {
                        val item = active[i]
                        views.setViewVisibility(rowIds[i], View.VISIBLE)
                        views.setTextViewText(titleIds[i], item.title)
                        val subText = if (item.type == MediaType.Movie) {
                            context.getString(R.string.type_movie)
                        } else {
                            context.getString(R.string.episode_progress, item.watchedEpisodes, item.totalEpisodes)
                        }
                        views.setTextViewText(subIds[i], subText)
                        views.setOnClickPendingIntent(rowIds[i], openPi)

                        val canIncrement = item.watchedEpisodes < item.totalEpisodes
                        views.setViewVisibility(btnIds[i], if (canIncrement) View.VISIBLE else View.GONE)
                        if (canIncrement) {
                            val incIntent = Intent(context, WidgetIncrementReceiver::class.java).apply {
                                action = ACTION_INCREMENT
                                putExtra(EXTRA_ITEM_ID, item.id)
                            }
                            val incPi = PendingIntent.getBroadcast(
                                context,
                                item.id.hashCode(),
                                incIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            )
                            views.setOnClickPendingIntent(btnIds[i], incPi)
                        } else {
                            views.setOnClickPendingIntent(btnIds[i], null)
                        }
                    } else {
                        views.setViewVisibility(rowIds[i], View.GONE)
                    }
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

/**
 * Handles the widget's +1 button. Not exported, so only the explicit
 * PendingIntent created by [SerieeWidgetProvider] can reach it.
 */
class WidgetIncrementReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SerieeWidgetProvider.ACTION_INCREMENT) return
        val itemId = intent.getStringExtra(SerieeWidgetProvider.EXTRA_ITEM_ID) ?: return
        SerieeWidgetProvider.incrementFromWidget(context, itemId)
        SerieeWidgetProvider.notifyChanged(context)
    }
}
