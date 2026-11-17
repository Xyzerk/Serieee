package com.seriee.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import com.seriee.app.MainActivity
import com.seriee.app.R
import com.seriee.app.data.EpisodeProgressRules
import com.seriee.app.data.MediaItem
import com.seriee.app.data.MediaType
import com.seriee.app.data.SerieeRepository
import com.seriee.app.data.WatchEvent
import com.seriee.app.data.WatchHistoryRules
import com.seriee.app.data.WatchStatus
import com.seriee.app.debug.AppLogger
import com.seriee.app.ui.SeriePalette
import java.util.Calendar
import java.util.concurrent.Executors
import android.graphics.Color as AndroidColor

class SerieeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        scheduleRefresh(context)
        enqueueUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        enqueueUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelRefresh(context)
    }

    /** Result of a +1 tap, used to give the user immediate feedback. */
    data class IncrementResult(
        val title: String,
        val season: Int,
        val episode: Int,
        val movie: Boolean,
        val finished: Boolean,
    )

    companion object {
        const val ACTION_INCREMENT = "com.seriee.app.widget.ACTION_INCREMENT"
        const val ACTION_REFRESH = "com.seriee.app.widget.ACTION_REFRESH"
        const val EXTRA_ITEM_ID = "extra_item_id"

        /** Storage reads and RemoteViews work must stay off the main thread. */
        private val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "seriee-widget").apply { isDaemon = true }
        }

        fun notifyChanged(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SerieeWidgetProvider::class.java))
            if (ids.isEmpty()) return
            scheduleRefresh(context)
            enqueueUpdate(context, manager, ids)
        }

        /**
         * Applies a single +1 from the widget. Called by [WidgetIncrementReceiver],
         * which is not exported, so no other app can change watch progress.
         */
        fun incrementFromWidget(context: Context, itemId: String): IncrementResult? =
            runCatching {
                val repo = SerieeRepository(context)
                val items = repo.loadItems()
                val target = items.firstOrNull { it.id == itemId } ?: return@runCatching null
                // Only active, released titles can advance from the widget.
                if (target.status != WatchStatus.Watching) return@runCatching null
                val increment = EpisodeProgressRules.increment(target) ?: return@runCatching null
                val updated = increment.item
                val now = updated.updatedAt
                repo.saveItems(items.map { if (it.id == itemId) updated else it })
                repo.recordWatchToday()
                repo.saveHistory(
                    WatchHistoryRules.append(
                        repo.loadHistory(),
                        WatchEvent(
                            updated.id,
                            updated.title,
                            updated.type,
                            increment.season,
                            increment.episode,
                            count = 1,
                            watchedAt = now,
                        ),
                        now,
                    )
                )
                IncrementResult(
                    title = updated.title,
                    season = increment.season,
                    episode = increment.episode,
                    movie = updated.type == MediaType.Movie,
                    finished = updated.status == WatchStatus.Completed,
                )
            }.onFailure { err ->
                AppLogger.w("Widget", "Failed to increment episode from widget", err)
            }.getOrNull()

        /** Keeps relative dates and status changes fresh even if the app is closed. */
        fun scheduleRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val nextMidnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }
            runCatching {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC,
                    nextMidnight.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    refreshPendingIntent(context),
                )
            }.onFailure { err ->
                AppLogger.w("Widget", "Could not schedule widget refresh", err)
            }
        }

        fun cancelRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            runCatching { alarmManager.cancel(refreshPendingIntent(context)) }
        }

        private fun refreshPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun enqueueUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
        ) {
            if (appWidgetIds.isEmpty()) return
            val appContext = context.applicationContext
            executor.execute {
                runCatching {
                    val items = SerieeRepository(appContext).loadItems()
                    val palette = loadWidgetPalette(appContext)
                    for (id in appWidgetIds) {
                        updateWidget(appContext, appWidgetManager, id, items, palette)
                    }
                }.onFailure { err ->
                    AppLogger.w("Widget", "Failed to refresh widget", err)
                }
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            items: List<MediaItem>,
            palette: SeriePalette,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_seriee)
            val openPi = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_header, openPi)

            val density = context.resources.displayMetrics.density
            applyTheme(views, palette, appWidgetManager, appWidgetId, density)

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
                views.setViewVisibility(R.id.widget_subtitle, View.GONE)
                for (rId in rowIds) views.setViewVisibility(rId, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_empty, View.GONE)
                views.setViewVisibility(R.id.widget_subtitle, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_subtitle,
                    context.getString(R.string.widget_watching_count, active.size),
                )
                for (i in rowIds.indices) {
                    if (i < active.size) {
                        val item = active[i]
                        views.setViewVisibility(rowIds[i], View.VISIBLE)
                        views.setTextViewText(titleIds[i], item.title)
                        val subText = subtitleFor(context, item)
                        views.setTextViewText(subIds[i], subText)
                        views.setOnClickPendingIntent(rowIds[i], detailPendingIntent(context, item.id))
                        views.setContentDescription(
                            rowIds[i],
                            context.getString(R.string.widget_row_description, item.title, subText),
                        )

                        val canIncrement = EpisodeProgressRules.canIncrement(item)
                        views.setViewVisibility(btnIds[i], if (canIncrement) View.VISIBLE else View.GONE)
                        if (canIncrement) {
                            views.setOnClickPendingIntent(btnIds[i], incrementPendingIntent(context, item.id))
                            views.setContentDescription(
                                btnIds[i],
                                context.getString(R.string.widget_increment_description, item.title),
                            )
                        } else {
                            views.setOnClickPendingIntent(btnIds[i], null)
                            views.setContentDescription(btnIds[i], null)
                        }
                    } else {
                        views.setViewVisibility(rowIds[i], View.GONE)
                        views.setContentDescription(rowIds[i], null)
                    }
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun subtitleFor(context: Context, item: MediaItem): String = when {
            item.type == MediaType.Movie -> context.getString(R.string.type_movie)
            item.seasons.isNotEmpty() -> context.getString(
                R.string.widget_series_progress,
                item.season,
                item.watchedEpisodes,
                item.totalEpisodes,
            )
            else -> context.getString(R.string.episode_progress, item.watchedEpisodes, item.totalEpisodes)
        }

        /**
         * Paints every surface with the resolved palette. RemoteViews cannot
         * tint shape drawables, so rounded cards are rasterised to bitmaps.
         */
        private fun applyTheme(
            views: RemoteViews,
            palette: SeriePalette,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            density: Float,
        ) {
            // RemoteViews travel over a binder transaction (~1 MB), so surfaces
            // are rendered at a modest resolution and scaled up by the launcher.
            val renderDensity = density.coerceIn(1f, 1.25f)
            fun dp(value: Float): Float = value * renderDensity
            fun px(value: Float): Int = dp(value).toInt().coerceAtLeast(2)

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 300)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 170)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 220)
            val widthDp = ((minWidth + maxWidth) / 2f).coerceIn(180f, 320f)
            val heightDp = ((minHeight + maxHeight) / 2f).coerceIn(140f, 260f)
            val widthPx = px(widthDp)
            val heightPx = px(heightDp)

            val neo = palette.isNeoBrutalist
            val shadow = if (neo) AndroidColor.BLACK else null
            val shadowOffset = if (neo) dp(3f) else 0f

            views.setImageViewBitmap(
                R.id.widget_surface_bg,
                widgetSurface(
                    widthPx = widthPx,
                    heightPx = heightPx,
                    fill = palette.surface.toArgb(),
                    border = palette.border.toArgb(),
                    borderPx = if (neo) dp(2.5f) else dp(1f),
                    radiusPx = if (neo) dp(8f) else dp(16f),
                    shadow = shadow,
                    shadowOffsetPx = shadowOffset,
                ),
            )

            val rowWidth = (widthPx - dp(20f)).toInt().coerceAtLeast(120)
            val rowBitmap = widgetSurface(
                widthPx = rowWidth,
                heightPx = px(48f),
                fill = palette.card.toArgb(),
                border = palette.border.toArgb(),
                borderPx = if (neo) dp(2f) else dp(1f),
                radiusPx = if (neo) dp(8f) else dp(10f),
                shadow = shadow,
                shadowOffsetPx = if (neo) dp(2f) else 0f,
            )
            val buttonBitmap = widgetSurface(
                widthPx = px(44f),
                heightPx = px(44f),
                fill = palette.primary.toArgb(),
                border = palette.border.toArgb(),
                borderPx = if (neo) dp(2f) else 0f,
                radiusPx = if (neo) dp(6f) else dp(8f),
                shadow = shadow,
                shadowOffsetPx = if (neo) dp(2f) else 0f,
            )

            val rowBgs = intArrayOf(R.id.widget_row_bg_0, R.id.widget_row_bg_1, R.id.widget_row_bg_2)
            val incBgs = intArrayOf(R.id.widget_item_inc_bg_0, R.id.widget_item_inc_bg_1, R.id.widget_item_inc_bg_2)
            val incTexts = intArrayOf(R.id.widget_item_inc_text_0, R.id.widget_item_inc_text_1, R.id.widget_item_inc_text_2)
            val titles = intArrayOf(R.id.widget_item_title_0, R.id.widget_item_title_1, R.id.widget_item_title_2)
            val subs = intArrayOf(R.id.widget_item_sub_0, R.id.widget_item_sub_1, R.id.widget_item_sub_2)

            for (i in rowBgs.indices) {
                views.setImageViewBitmap(rowBgs[i], rowBitmap)
                views.setImageViewBitmap(incBgs[i], buttonBitmap)
                views.setTextColor(titles[i], palette.text.toArgb())
                views.setTextColor(subs[i], palette.muted.toArgb())
                views.setTextColor(incTexts[i], palette.onAccent.toArgb())
            }
            views.setTextColor(R.id.widget_title, palette.text.toArgb())
            views.setTextColor(R.id.widget_subtitle, palette.muted.toArgb())
            views.setTextColor(R.id.widget_empty, palette.muted.toArgb())
        }

        private fun detailPendingIntent(context: Context, itemId: String): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = itemUri(itemId)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            return PendingIntent.getActivity(
                context,
                itemId.hashCode() + 101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun incrementPendingIntent(context: Context, itemId: String): PendingIntent {
            val intent = Intent(context, WidgetIncrementReceiver::class.java).apply {
                action = ACTION_INCREMENT
                data = itemUri(itemId)
                putExtra(EXTRA_ITEM_ID, itemId)
            }
            return PendingIntent.getBroadcast(
                context,
                itemId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /** Unique per-item data URI so two items can never share a PendingIntent. */
        private fun itemUri(itemId: String): Uri =
            Uri.parse("seriee://item/${Uri.encode(itemId)}")
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
        val pending = goAsync()
        executor.execute {
            try {
                val result = SerieeWidgetProvider.incrementFromWidget(context, itemId)
                SerieeWidgetProvider.notifyChanged(context)
                if (result != null) {
                    val message = when {
                        result.finished -> context.getString(R.string.widget_finished, result.title)
                        result.movie -> context.getString(R.string.widget_movie_watched, result.title)
                        else -> context.getString(
                            R.string.widget_episode_watched,
                            result.title,
                            result.season,
                            result.episode,
                        )
                    }
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        /** Storage and RemoteViews work must stay off the broadcast main thread. */
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "seriee-widget").apply { isDaemon = true }
        }
    }
}

/** Daily refresh so relative progress never goes stale while the app is closed. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SerieeWidgetProvider.ACTION_REFRESH) return
        SerieeWidgetProvider.notifyChanged(context)
    }
}
