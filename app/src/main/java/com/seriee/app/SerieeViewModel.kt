package com.seriee.app

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seriee.app.data.AniListListClient
import com.seriee.app.data.AiringApply
import com.seriee.app.data.AiringRefresher
import com.seriee.app.data.AppLanguage
import com.seriee.app.data.AppThemeMode
import com.seriee.app.data.AppThemeStyle
import com.seriee.app.data.CustomThemeExtras
import com.seriee.app.data.EpisodeIncrement
import com.seriee.app.data.EpisodeProgressRules
import com.seriee.app.data.ImportError
import com.seriee.app.data.ImportException
import com.seriee.app.data.ImportMapper
import com.seriee.app.data.ImportMode
import com.seriee.app.data.ImportPhase
import com.seriee.app.data.ImportSource
import com.seriee.app.data.ImportUiState
import com.seriee.app.data.MalXmlParser
import com.seriee.app.data.MediaItem
import com.seriee.app.data.episodeKey
import com.seriee.app.data.MetadataRepository
import com.seriee.app.data.PosterStore
import com.seriee.app.data.SerieeRepository
import com.seriee.app.data.StreakRules
import com.seriee.app.data.StreakState
import com.seriee.app.data.ThemePresetCodec
import com.seriee.app.ui.NeoBrutalistColors
import com.seriee.app.ui.NeoTone
import com.seriee.app.data.WatchEvent
import com.seriee.app.data.WatchHistoryRules
import com.seriee.app.data.WatchStatus
import com.seriee.app.widget.SerieeWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SerieeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SerieeRepository(app)

    var items by mutableStateOf(repo.loadItems())
        private set
    private val _themeMode = mutableStateOf(repo.loadThemeMode())
    val themeMode: AppThemeMode get() = _themeMode.value
    private val _themeStyle = mutableStateOf(repo.loadThemeStyle())
    val themeStyle: AppThemeStyle get() = _themeStyle.value
    private val _neoTone = mutableStateOf(
        NeoTone.fromName(repo.loadNeoTone()).also { NeoBrutalistColors.setTone(it) }
    )
    val neoTone: NeoTone get() = _neoTone.value
    private val _customAccent = mutableStateOf(Color(repo.loadCustomAccent()))
    val customAccent: Color get() = _customAccent.value
    private val _customBackgroundStart = mutableStateOf(Color(repo.loadCustomBackgroundStart()))
    val customBackgroundStart: Color get() = _customBackgroundStart.value
    private val _customBackgroundEnd = mutableStateOf(Color(repo.loadCustomBackgroundEnd()))
    val customBackgroundEnd: Color get() = _customBackgroundEnd.value
    private val _customGradient = mutableStateOf(repo.loadCustomGradient())
    val customGradient: Boolean get() = _customGradient.value
    private val _customGlass = mutableStateOf(repo.loadCustomGlass())
    val customGlass: Boolean get() = _customGlass.value
    private val _customHighContrast = mutableStateOf(repo.loadCustomHighContrast())
    val customHighContrast: Boolean get() = _customHighContrast.value
    private val _customExtras = mutableStateOf(repo.loadCustomExtras())
    val customExtras: CustomThemeExtras get() = _customExtras.value
    private val _reduceTransparency = mutableStateOf(repo.loadReduceTransparency())
    val reduceTransparency: Boolean get() = _reduceTransparency.value
    private val _batterySaverAmoled = mutableStateOf(repo.loadBatterySaverAmoled())
    val batterySaverAmoled: Boolean get() = _batterySaverAmoled.value
    private val _gridView = mutableStateOf(repo.loadGridView())
    val gridView: Boolean get() = _gridView.value
    private val _language = mutableStateOf(repo.loadLanguage())
    val language: AppLanguage get() = _language.value
    private val _tmdbKey = mutableStateOf(repo.loadTmdbKey())
    val tmdbKey: String get() = _tmdbKey.value
    private val _omdbKey = mutableStateOf(repo.loadOmdbKey())
    val omdbKey: String get() = _omdbKey.value

    private val _streakDays = mutableStateOf(repo.loadStreakDays())
    val streakDays: Int get() = _streakDays.value
    private val _bestStreak = mutableStateOf(repo.loadBestStreak())
    val bestStreak: Int get() = _bestStreak.value
    private val _watchDays = mutableStateOf(repo.loadWatchDays())
    val watchDays: Set<String> get() = _watchDays.value

    var history by mutableStateOf(repo.loadHistory())
        private set

    private val _showWelcome = mutableStateOf(false)
    val showWelcome: Boolean get() = _showWelcome.value
    private val _showChangelog = mutableStateOf(false)
    val showChangelog: Boolean get() = _showChangelog.value

    var showBackupReminder by mutableStateOf(repo.loadPendingBackupPrompt())
        private set

    private val aniListClient = AniListListClient()
    private val metadataRepo = MetadataRepository()
    private val refresher = AiringRefresher(metadataRepo::fetchAiring, metadataRepo::fetchTvmazeAiring)

    @Volatile
    private var airingRefreshStarted = false
    private var importJob: Job? = null
    private val _importState = mutableStateOf(ImportUiState())
    val importState: ImportUiState get() = _importState.value

    // Single source of truth: versionCode/versionName in app/build.gradle.kts,
    // generated into BuildConfig. No hardcoded fallbacks here.
    val currentVersionCode: Int = BuildConfig.VERSION_CODE
    val currentVersionName: String = BuildConfig.VERSION_NAME

    @Volatile
    private var latestActivePosters: Set<String> = emptySet()
    private var maintenanceJob: Job? = null
    private var widgetThemeJob: Job? = null

    private fun commit(newItems: List<MediaItem>) {
        val uniqueItems = newItems.distinctBy { it.id }
        items = uniqueItems
        repo.saveItems(uniqueItems)
        latestActivePosters = uniqueItems
            .mapNotNull { it.poster.takeIf { p -> p.isNotBlank() } }
            .toSet()
        scheduleMaintenance()
    }

    /**
     * Poster pruning and widget refreshes touch the file system and RemoteViews,
     * so they run off the main thread. Debouncing coalesces bursts (imports,
     * mark-all-watched) and guarantees the prune sees the newest poster set.
     */
    private fun scheduleMaintenance() {
        maintenanceJob?.cancel()
        val app = getApplication<Application>()
        maintenanceJob = viewModelScope.launch(Dispatchers.IO) {
            delay(200)
            PosterStore.pruneUnused(app, latestActivePosters)
            SerieeWidgetProvider.notifyChanged(app)
        }
    }

    /**
     * The launcher widget mirrors the resolved palette. Theme edits arrive on
     * every slider or picker move, so debounce the burst into a single rebuild;
     * [SerieeWidgetProvider.notifyChanged] is a no-op when no widget is placed.
     */
    private fun scheduleWidgetThemeRefresh() {
        widgetThemeJob?.cancel()
        val app = getApplication<Application>()
        widgetThemeJob = viewModelScope.launch(Dispatchers.IO) {
            delay(250)
            SerieeWidgetProvider.notifyChanged(app)
        }
    }

    fun dismissBackupReminder() {
        repo.savePendingBackupPrompt(false)
        showBackupReminder = false
    }

    /** Re-reads storage after the widget changed it. */
    fun refreshFromStorage() {
        val storedItems = repo.loadItems()
        val storedHistory = repo.loadHistory()
        val storedStreakDays = repo.loadStreakDays()
        val storedBestStreak = repo.loadBestStreak()
        val storedWatchDays = repo.loadWatchDays()
        if (storedItems != items) items = storedItems
        if (storedHistory != history) history = storedHistory
        if (storedStreakDays != _streakDays.value) _streakDays.value = storedStreakDays
        if (storedBestStreak != _bestStreak.value) _bestStreak.value = storedBestStreak
        if (storedWatchDays != _watchDays.value) _watchDays.value = storedWatchDays
    }

    private fun maybeTriggerBackupReminder(itemCount: Int, importedDelta: Int = 0) {
        if (showBackupReminder || repo.loadPendingBackupPrompt()) return
        val sevenDaysMs = 7L * 86_400_000L
        val lastBackup = repo.loadLastBackupAt()
        val backupIsStale = lastBackup == 0L || (System.currentTimeMillis() - lastBackup) > sevenDaysMs
        if (!backupIsStale) return
        val sinceLastPrompt = itemCount - repo.loadBackupPromptedAtCount()
        val enoughNewItems = itemCount >= 10 && sinceLastPrompt >= 10
        if (importedDelta >= 3 || enoughNewItems) {
            repo.savePendingBackupPrompt(true)
            repo.saveBackupPromptedAtCount(itemCount)
            showBackupReminder = true
        }
    }

    init {
        com.seriee.app.reminder.ReminderScheduler.createNotificationChannel(app)
        val lastSeen = repo.loadLastSeenVersionCode()
        // First run: show the welcome slides. People who already use the app
        // (existing library or a previously seen version) are not shown them.
        if (!repo.loadOnboardingDone()) {
            if (items.isNotEmpty() || lastSeen > 0) repo.saveOnboardingDone(true)
            else _showWelcome.value = true
        }
        // Show changelog once per version upgrade (never on top of the welcome)
        if (!_showWelcome.value && lastSeen < currentVersionCode) {
            _showChangelog.value = true
        }
        // Refresh alarms for any pending items (the scheduler picks the target)
        items.forEach { item ->
            if (item.reminderEnabled) {
                com.seriee.app.reminder.ReminderScheduler.scheduleReminder(app, item)
            }
        }
        startAiringRefresh()
    }

    /**
     * One airing refresh per app open, batched across sources (AniList
     * `id_in`, TVmaze on stale items). Merges updates onto the latest list;
     * repaired items get their pending alarms cancelled.
     */
    private fun startAiringRefresh() {
        if (airingRefreshStarted) return
        airingRefreshStarted = true
        if (items.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { refresher.refresh(items) }.getOrNull() ?: return@launch
            withContext(Dispatchers.Main) {
                val applied = AiringApply.apply(items, result)
                if (!applied.changed) return@withContext
                commit(applied.items)
                applied.items.forEach { item ->
                    if (item.reminderEnabled && result.updates.containsKey(item.id)) {
                        com.seriee.app.reminder.ReminderScheduler.scheduleReminder(app, item)
                    }
                }
                applied.repairedIds.forEach { com.seriee.app.reminder.ReminderScheduler.cancelReminder(app, it) }
            }
        }
    }

    fun incrementEpisode(id: String) {
        val target = items.find { it.id == id } ?: return
        val before = target.watchedEpisodes
        var logged: EpisodeIncrement? = null
        commit(items.map { m ->
            if (m.id != id) return@map m
            val increment = EpisodeProgressRules.increment(m)
            if (increment == null) m else {
                logged = increment
                increment.item
            }
        })
        val after = items.find { it.id == id } ?: return
        if (after.watchedEpisodes > before) {
            logged?.let { logWatch(target, it.season, it.episode) }
            recordWatch()
            pushProgressToAniList(after)
        }
    }

    fun setCurrentEpisode(id: String, seasonNo: Int, epNo: Int) {
        val target = items.find { it.id == id } ?: return
        val before = target.watchedEpisodes
        commit(items.map { m ->
            if (m.id != id) return@map m
            if (m.seasons.isNotEmpty()) {
                val newSeasons = m.seasons.map { s ->
                    if (s.number == seasonNo) s.copy(currentEp = epNo.coerceIn(0, s.totalEp)) else s
                }
                val watched = newSeasons.sumOf { it.currentEp }
                val total = newSeasons.sumOf { it.totalEp }
                m.copy(
                    seasons = newSeasons,
                    season = seasonNo,
                    currentEp = watched,
                    totalEp = total.coerceAtLeast(1),
                    status = statusAfterProgress(m.status, watched, total),
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                val next = epNo.coerceIn(0, m.totalEp)
                m.copy(
                    currentEp = next,
                    status = statusAfterProgress(m.status, next, m.totalEp),
                    updatedAt = System.currentTimeMillis()
                )
            }
        })
        val after = items.find { it.id == id } ?: return
        val delta = after.watchedEpisodes - before
        if (delta > 0) {
            val loggedEpisode = if (after.seasons.isNotEmpty()) {
                after.seasons.firstOrNull { it.number == seasonNo }?.currentEp ?: epNo
            } else {
                after.currentEp
            }
            logWatch(target, seasonNo, loggedEpisode, delta)
            recordWatch()
        }
    }

    /** Completed when everything is watched; a finished item moved backwards becomes active again. */
    private fun statusAfterProgress(current: WatchStatus, watched: Int, total: Int): WatchStatus = when {
        total > 0 && watched >= total -> WatchStatus.Completed
        current == WatchStatus.Completed -> WatchStatus.Watching
        else -> current
    }

    /** Marks every episode of [seasonNum] as watched in one step. */
    fun markSeasonComplete(id: String, seasonNum: Int) {
        val target = items.firstOrNull { it.id == id } ?: return
        val sec = target.effectiveSeasons.firstOrNull { it.number == seasonNum } ?: return
        setCurrentEpisode(id, seasonNum, sec.totalEp)
    }

    private fun logWatch(item: MediaItem, season: Int, episode: Int, count: Int = 1) {
        if (count <= 0) return
        val event = WatchEvent(
            itemId = item.id,
            title = item.title,
            type = item.type,
            season = season,
            episode = episode,
            count = count,
            watchedAt = System.currentTimeMillis()
        )
        history = WatchHistoryRules.trim(history + event)
        repo.saveHistory(history)
    }

    fun togglePin(id: String) {
        commit(items.map { if (it.id == id) it.copy(pinned = !it.pinned) else it })
    }

    /** F5 (Discover): add a metadata result straight to the library as Plan to Watch / Coming Soon. */
    fun addFromMetadata(r: com.seriee.app.data.MetadataResult) {
        if (items.any { it.source == r.source && it.sourceId == r.sourceId && r.sourceId.isNotBlank() }) return
        val seasons = r.seasonEpisodeCounts.mapIndexed { idx, total ->
            com.seriee.app.data.Season(
                number = r.seasonNumbers.getOrNull(idx)?.takeIf { it > 0 } ?: (idx + 1),
                currentEp = 0,
                totalEp = total.coerceAtLeast(1)
            )
        }
        val item = MediaItem(
            id = java.util.UUID.randomUUID().toString(),
            title = r.title,
            type = r.type,
            status = if (r.upcoming) WatchStatus.ComingSoon else WatchStatus.PlanToWatch,
            poster = r.posterUrl,
            description = r.description,
            source = r.source,
            sourceId = r.sourceId,
            tags = r.tags,
            altTitles = r.altTitles,
            seasons = seasons,
            totalEp = seasons.sumOf { it.totalEp }.coerceAtLeast(1),
            releaseDate = r.releaseAt,
            airing = r.airing,
            airingStarted = r.airingStarted,
            nextEpisode = r.nextEpisode,
            nextEpisodeSeason = r.nextEpisodeSeason,
            nextAirAt = r.nextAirAt,
            episodeDurationMin = r.episodeDurationMin
        )
        commit(items + item)
    }

    /** F1: set (or clear, with 0) the star rating for one episode. */
    fun setEpisodeRating(id: String, season: Int, episode: Int, rating: Int) {
        val key = episodeKey(season, episode)
        commit(items.map { m ->
            if (m.id != id) m else {
                val next = if (rating in 1..5) m.episodeRatings + (key to rating) else m.episodeRatings - key
                m.copy(episodeRatings = next)
            }
        })
    }

    /** F1: set (or clear, with blank text) the note for one episode. */
    fun setEpisodeNote(id: String, season: Int, episode: Int, note: String) {
        val key = episodeKey(season, episode)
        val trimmed = note.trim()
        commit(items.map { m ->
            if (m.id != id) m else {
                val next = if (trimmed.isNotEmpty()) m.episodeNotes + (key to trimmed) else m.episodeNotes - key
                m.copy(episodeNotes = next)
            }
        })
    }

    /** F1: count one more rewatch of a completed title. */
    fun addRewatch(id: String) {
        commit(items.map { if (it.id == id) it.copy(rewatchCount = it.rewatchCount + 1) else it })
    }

    /** F6: add or remove a title from a user-defined custom list. */
    fun toggleCustomList(id: String, listName: String) {
        val name = listName.trim()
        if (name.isEmpty()) return
        commit(items.map { m ->
            if (m.id != id) m else {
                val next = if (m.lists.any { it.equals(name, ignoreCase = true) }) {
                    m.lists.filterNot { it.equals(name, ignoreCase = true) }
                } else {
                    (m.lists + name).distinctBy { it.lowercase() }
                }
                m.copy(lists = next)
            }
        })
    }

    /** F6: every custom list name in use, sorted. */
    fun customListNames(): List<String> =
        items.flatMap { it.lists }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }

    /**
     * Daily streak. Watch activity is bucketed by local calendar day:
     * same day = no change, yesterday = +1, gap = reset to 1.
     */
    private fun recordWatch() {
        val next = StreakRules.advance(
            StreakState(
                streakDays = _streakDays.value,
                bestStreak = _bestStreak.value,
                lastWatchDay = repo.loadLastWatchDay(),
                watchDays = _watchDays.value,
            )
        )
        _streakDays.value = next.streakDays
        _bestStreak.value = next.bestStreak
        _watchDays.value = next.watchDays
        repo.saveStreak(next.streakDays, next.bestStreak, next.lastWatchDay, next.watchDays)
    }

    fun resetStreak() {
        _streakDays.value = 0
        _bestStreak.value = 0
        _watchDays.value = emptySet()
        repo.saveStreak(0, 0, "", emptySet())
    }

    /** Clearing history is separate from the streak and never touches progress. */
    fun clearHistory() {
        history = emptyList()
        repo.clearHistory()
    }

    fun toggleReminder(id: String) {
        commit(items.map { m ->
            if (m.id == id) {
                val newStatus = !m.reminderEnabled
                val updated = m.copy(reminderEnabled = newStatus)
                if (newStatus) {
                    com.seriee.app.reminder.ReminderScheduler.scheduleReminder(getApplication(), updated)
                } else {
                    com.seriee.app.reminder.ReminderScheduler.cancelReminder(getApplication(), id)
                }
                updated
            } else m
        })
    }

    fun testReminder(item: MediaItem) {
        val app = getApplication<Application>()
        com.seriee.app.reminder.ReminderScheduler.sendImmediateNotification(
            app,
            item.title,
            app.getString(
                R.string.reminder_test_message,
                app.getString(item.type.labelRes),
                item.title
            ),
            item.id
        )
    }

    fun upsert(item: MediaItem) {
        val synced = if (item.seasons.isNotEmpty()) {
            val watched = item.seasons.sumOf { it.currentEp.coerceIn(0, it.totalEp) }
            val total = item.seasons.sumOf { it.totalEp.coerceAtLeast(1) }
            val active = item.seasons.firstOrNull { it.currentEp < it.totalEp } ?: item.seasons.lastOrNull()
            item.copy(season = active?.number ?: item.season, currentEp = watched, totalEp = total.coerceAtLeast(1))
        } else item
        val exists = items.any { it.id == synced.id }
        commit(if (exists) items.map { if (it.id == synced.id) synced else it } else listOf(synced) + items)

        if (!exists) {
            maybeTriggerBackupReminder(items.size)
        }

        if (synced.reminderEnabled) {
            com.seriee.app.reminder.ReminderScheduler.scheduleReminder(getApplication(), synced)
        } else {
            com.seriee.app.reminder.ReminderScheduler.cancelReminder(getApplication(), synced.id)
        }
    }

    fun delete(id: String) {
        com.seriee.app.reminder.ReminderScheduler.cancelReminder(getApplication(), id)
        items.find { it.id == id }?.poster?.let {
            PosterStore.deleteIfLocal(getApplication(), it)
        }
        commit(items.filterNot { it.id == id })
    }

    // F3: AniList sync token + outbound progress push.
    private val _anilistToken = mutableStateOf(repo.loadAniListToken())
    val anilistToken: String get() = _anilistToken.value

    fun setAniListToken(value: String) {
        _anilistToken.value = value.trim()
        repo.saveAniListToken(value.trim())
    }

    /** Fire-and-forget: push this item's progress to AniList when possible. */
    fun pushProgressToAniList(item: MediaItem) {
        val token = _anilistToken.value
        if (token.isBlank()) return
        if (!item.source.equals("AniList", ignoreCase = true)) return
        val mediaId = item.sourceId.toIntOrNull() ?: return
        val progress = item.watchedEpisodes
        val status = item.status
        val score = item.rating
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                com.seriee.app.data.AniListSyncClient.pushProgress(token, mediaId, progress, status, score)
            }
        }
    }

    fun setTmdbKey(value: String) {
        _tmdbKey.value = value.trim()
        repo.saveTmdbKey(value.trim())
    }

    fun setOmdbKey(value: String) {
        _omdbKey.value = value.trim()
        repo.saveOmdbKey(value.trim())
    }

    /** Closes the welcome slides (finished or skipped) and never shows them again. */
    fun finishWelcome() {
        _showWelcome.value = false
        repo.saveOnboardingDone(true)
        repo.saveLastSeenVersionCode(currentVersionCode)
    }

    /** Replays the welcome slides on demand. */
    fun openWelcome() {
        _showWelcome.value = true
    }

    fun openChangelog() {
        _showChangelog.value = true
    }

    fun dismissChangelog() {
        _showChangelog.value = false
        repo.saveLastSeenVersionCode(currentVersionCode)
    }

    fun setThemeMode(value: AppThemeMode) {
        if (_themeMode.value == value) return
        _themeMode.value = value
        repo.saveTheme(_themeMode.value, _themeStyle.value)
        scheduleWidgetThemeRefresh()
    }

    /** Falls back to the app defaults after repeated crashes and clears the crash streak. */
    fun resetThemeToDefault() {
        setThemeMode(AppThemeMode.System)
        setThemeStyle(AppThemeStyle.Classic)
        setNeoTone(NeoTone.Yellow)
        setReduceTransparency(false)
        com.seriee.app.debug.AppLogger.dismissPendingCrash()
        val ctx = getApplication<Application>()
        Toast.makeText(ctx, ctx.getString(R.string.crash_theme_reset_done), Toast.LENGTH_SHORT).show()
    }

    fun setThemeStyle(value: AppThemeStyle) {
        if (_themeStyle.value == value) return
        _themeStyle.value = value
        repo.saveTheme(_themeMode.value, _themeStyle.value)
        scheduleWidgetThemeRefresh()
    }

    fun setNeoTone(value: NeoTone) {
        if (_neoTone.value == value) return
        _neoTone.value = value
        NeoBrutalistColors.setTone(value)
        repo.saveNeoTone(value.name)
        scheduleWidgetThemeRefresh()
    }

    fun setCustomAccent(value: Color) {
        _customAccent.value = value
        repo.saveCustomAccent(value.toArgb())
        ensureCustomStyle()
        scheduleWidgetThemeRefresh()
    }

    fun setCustomBackground(start: Color, end: Color) {
        _customBackgroundStart.value = start
        _customBackgroundEnd.value = end
        repo.saveCustomBackground(start.toArgb(), end.toArgb())
        ensureCustomStyle()
        scheduleWidgetThemeRefresh()
    }

    fun setCustomGradient(value: Boolean) {
        _customGradient.value = value
        repo.saveCustomGradient(value)
        ensureCustomStyle()
        scheduleWidgetThemeRefresh()
    }

    /** Glass and high contrast are two different surface treatments. */
    fun setCustomGlass(value: Boolean) {
        _customGlass.value = value
        repo.saveCustomGlass(value)
        if (value && _customHighContrast.value) {
            _customHighContrast.value = false
            repo.saveCustomHighContrast(false)
        }
        ensureCustomStyle()
        scheduleWidgetThemeRefresh()
    }

    /** Neo-Brutalist treatment; mutually exclusive with glass surfaces. */
    fun setCustomHighContrast(value: Boolean) {
        _customHighContrast.value = value
        repo.saveCustomHighContrast(value)
        if (value && _customGlass.value) {
            _customGlass.value = false
            repo.saveCustomGlass(false)
        }
        ensureCustomStyle()
        scheduleWidgetThemeRefresh()
    }

    /** Updates the fine-grained overrides (per-token colours, image tuning). */
    fun updateCustomExtras(transform: (CustomThemeExtras) -> CustomThemeExtras) {
        val next = transform(_customExtras.value)
        _customExtras.value = next
        repo.saveCustomExtras(next)
        ensureCustomStyle()
        scheduleWidgetThemeRefresh()
    }

    /**
     * Copies the picked image into app storage (downscaled to the screen) so the
     * background keeps working after the original is moved or deleted.
     */
    fun setCustomBackgroundImage(context: Context, uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { importBackgroundImage(app, uri) }.getOrNull()
            }
            if (saved == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.theme_image_failed),
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            val old = _customExtras.value.imagePath
            updateCustomExtras { it.copy(imagePath = saved, imageEnabled = true) }
            if (old != null && old != saved) java.io.File(old).delete()
        }
    }

    fun removeCustomBackgroundImage() {
        val old = _customExtras.value.imagePath
        updateCustomExtras { it.copy(imagePath = null, imageEnabled = false) }
        if (old != null) viewModelScope.launch(Dispatchers.IO) { java.io.File(old).delete() }
    }

    /** Clears every per-token colour override but keeps the picked image. */
    fun resetCustomColorOverrides() {
        updateCustomExtras {
            it.copy(
                text = null, muted = null, card = null, surface = null, border = null,
                success = null, warning = null, danger = null, info = null,
                reminder = null, star = null
            )
        }
    }

    private fun importBackgroundImage(app: Application, uri: Uri): String {
        val resolver = app.contentResolver
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        val metrics = app.resources.displayMetrics
        val target = maxOf(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1080)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, opts)
        } ?: error("decode failed")
        val dir = java.io.File(app.filesDir, "theme").apply { mkdirs() }
        val file = java.io.File(dir, "background-${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return file.absolutePath
    }

    /** Editing any custom colour moves the app onto the Custom style. */
    private fun ensureCustomStyle() {
        if (_themeStyle.value != AppThemeStyle.Custom) setThemeStyle(AppThemeStyle.Custom)
    }

    fun setReduceTransparency(value: Boolean) {
        _reduceTransparency.value = value
        repo.saveReduceTransparency(value)
        scheduleWidgetThemeRefresh()
    }

    fun setBatterySaverAmoled(value: Boolean) {
        _batterySaverAmoled.value = value
        repo.saveBatterySaverAmoled(value)
    }

    /** Portable text snapshot of the current theme, shared through the clipboard. */
    fun themeCode(): String = ThemePresetCodec.encode(
        com.seriee.app.data.ThemePreset(
            mode = _themeMode.value,
            style = _themeStyle.value,
            accent = _customAccent.value.toArgb().toLong() and 0xFFFFFFFFL,
            backgroundStart = _customBackgroundStart.value.toArgb().toLong() and 0xFFFFFFFFL,
            backgroundEnd = _customBackgroundEnd.value.toArgb().toLong() and 0xFFFFFFFFL,
            gradient = _customGradient.value,
            glass = _customGlass.value,
            highContrast = _customHighContrast.value,
            reduceTransparency = _reduceTransparency.value,
            neoTone = _neoTone.value.name,
            extras = _customExtras.value.copy(imagePath = null, imageEnabled = false)
        )
    )

    /** Applies a pasted theme code. Returns false when the text is not a theme. */
    fun applyThemeCode(raw: String?): Boolean {
        val preset = ThemePresetCodec.decode(raw) ?: return false
        _themeMode.value = preset.mode
        _themeStyle.value = preset.style
        _customAccent.value = Color(preset.accent.toInt())
        _customBackgroundStart.value = Color(preset.backgroundStart.toInt())
        _customBackgroundEnd.value = Color(preset.backgroundEnd.toInt())
        _customGradient.value = preset.gradient
        _customGlass.value = preset.glass
        _customHighContrast.value = preset.highContrast
        _reduceTransparency.value = preset.reduceTransparency
        NeoTone.fromName(preset.neoTone).let { tone ->
            _neoTone.value = tone
            NeoBrutalistColors.setTone(tone)
            repo.saveNeoTone(tone.name)
        }
        repo.saveTheme(preset.mode, preset.style)
        repo.saveCustomAccent(_customAccent.value.toArgb())
        repo.saveCustomBackground(
            _customBackgroundStart.value.toArgb(),
            _customBackgroundEnd.value.toArgb()
        )
        repo.saveCustomGradient(preset.gradient)
        repo.saveCustomGlass(preset.glass)
        repo.saveCustomHighContrast(preset.highContrast)
        repo.saveReduceTransparency(preset.reduceTransparency)
        // A code never carries the image file, so keep the one already on this device.
        val current = _customExtras.value
        _customExtras.value = preset.extras.copy(
            imagePath = current.imagePath,
            imageEnabled = current.imageEnabled
        )
        repo.saveCustomExtras(_customExtras.value)
        scheduleWidgetThemeRefresh()
        return true
    }

    fun setGridView(value: Boolean) {
        _gridView.value = value
        repo.saveGridView(value)
    }

    fun setLanguage(value: AppLanguage) {
        if (_language.value == value) return
        _language.value = value
        repo.saveLanguage(value)
        val locales = if (value.tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(value.tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    // ------------------------------------------------------------- import UX

    fun beginImport() {
        importJob?.cancel()
        val current = _importState.value
        _importState.value = ImportUiState(source = current.source, mode = current.mode)
    }

    fun setImportSource(source: ImportSource) {
        importJob?.cancel()
        _importState.value = _importState.value.copy(
            source = source,
            phase = ImportPhase.Idle,
            fileName = "",
            preview = null,
            result = null,
            error = null
        )
    }

    fun setImportMode(mode: ImportMode) {
        _importState.value = _importState.value.copy(mode = mode)
    }

    fun fetchAniList(username: String) {
        importJob?.cancel()
        _importState.value = _importState.value.copy(
            phase = ImportPhase.Loading,
            preview = null,
            result = null,
            error = null
        )
        importJob = viewModelScope.launch {
            try {
                val rows = aniListClient.fetch(username)
                val mapped = ImportMapper.fromAniList(rows)
                if (mapped.isEmpty()) throw ImportException(ImportError.ListPrivateOrEmpty)
                _importState.value = _importState.value.copy(
                    phase = ImportPhase.Preview,
                    preview = ImportMapper.preview(mapped, items)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ImportException) {
                _importState.value = _importState.value.copy(phase = ImportPhase.Error, error = e.error)
            } catch (e: Exception) {
                _importState.value = _importState.value.copy(phase = ImportPhase.Error, error = ImportError.Unknown)
            }
        }
    }

    fun loadMalFile(context: Context, uri: Uri, displayName: String) {
        importJob?.cancel()
        _importState.value = _importState.value.copy(
            phase = ImportPhase.Loading,
            fileName = displayName,
            preview = null,
            result = null,
            error = null
        )
        importJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw ImportException(ImportError.FileInvalid)
                    ImportMapper.fromMal(MalXmlParser.parseBytes(bytes))
                }
                if (result.isEmpty()) throw ImportException(ImportError.NoEntries)
                _importState.value = _importState.value.copy(
                    phase = ImportPhase.Preview,
                    preview = ImportMapper.preview(result, items)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ImportException) {
                _importState.value = _importState.value.copy(phase = ImportPhase.Error, error = e.error)
            } catch (e: Exception) {
                _importState.value = _importState.value.copy(phase = ImportPhase.Error, error = ImportError.FileInvalid)
            }
        }
    }

    fun applyImport() {
        val preview = _importState.value.preview ?: return
        val result = if (_importState.value.mode == ImportMode.Replace) {
            // Replace drops the old library, so clear its pending alarms first.
            items.forEach { old ->
                if (old.reminderEnabled) {
                    com.seriee.app.reminder.ReminderScheduler.cancelReminder(getApplication(), old.id)
                }
            }
            val (finalItems, result) = ImportMapper.replace(preview.entries)
            commit(finalItems)
            result
        } else {
            val (finalItems, result) = ImportMapper.merge(preview.entries, items)
            commit(finalItems)
            result
        }
        _importState.value = _importState.value.copy(
            phase = ImportPhase.Done,
            preview = null,
            result = result
        )
        items.forEach { item ->
            if (item.reminderEnabled) {
                com.seriee.app.reminder.ReminderScheduler.scheduleReminder(getApplication(), item)
            }
        }
        maybeTriggerBackupReminder(items.size, importedDelta = result.added)
    }

    fun dismissImport() {
        importJob?.cancel()
        _importState.value = ImportUiState(source = _importState.value.source, mode = _importState.value.mode)
    }

    // F2: automatic backup settings + runner (called once on app start).
    var autoBackupEnabled by mutableStateOf(repo.loadAutoBackupEnabled())
        private set
    var autoBackupFolderUri by mutableStateOf(repo.loadAutoBackupFolderUri())
        private set

    fun setAutoBackupEnabled(value: Boolean) {
        autoBackupEnabled = value
        repo.saveAutoBackupEnabled(value)
    }

    fun setAutoBackupFolder(uriString: String) {
        autoBackupFolderUri = uriString
        repo.saveAutoBackupFolderUri(uriString)
    }

    /** Writes a backup into the picked folder when the last one is >24h old. */
    fun maybeAutoBackup(context: Context) {
        if (!autoBackupEnabled || autoBackupFolderUri.isBlank()) return
        val nowMs = System.currentTimeMillis()
        if (nowMs - repo.loadLastAutoBackupAt() < 24L * 60L * 60L * 1000L) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val treeUri = Uri.parse(autoBackupFolderUri)
                    val parentDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                        ?: error("folder")
                    val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                        .format(java.util.Date(nowMs))
                    val file = parentDoc.createFile("application/json", "seriee_autobackup_$stamp.json")
                        ?: error("file")
                    context.contentResolver.openOutputStream(file.uri)?.use { out ->
                        out.write(repo.exportBackupJson(items, history).toByteArray(Charsets.UTF_8))
                    } ?: error("stream")
                    repo.saveLastAutoBackupAt(nowMs)
                }
            }
        }
    }

    fun exportBackup(context: Context, uri: Uri): Result<Int> {
        return runCatching {
            val jsonString = repo.exportBackupJson(items, history)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error(getApplication<Application>().getString(R.string.error_open_output))
            items.size
        }.onSuccess {
            repo.saveLastBackupAt(System.currentTimeMillis())
            repo.savePendingBackupPrompt(false)
            repo.saveBackupPromptedAtCount(items.size)
            showBackupReminder = false
        }
    }

    fun importBackup(context: Context, uri: Uri, replace: Boolean): Result<Int> {
        return runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error(getApplication<Application>().getString(R.string.error_read_backup))
            val payload = repo.importBackupJson(raw)
            val imported = payload.items
            if (imported.isEmpty()) error(getApplication<Application>().getString(R.string.error_empty_backup))

            val finalItems = if (replace) {
                imported
            } else {
                val importedIds = imported.map { it.id }.toSet()
                imported + items.filterNot { it.id in importedIds }
            }
            commit(finalItems)
            // Replace restores the file's history (or clears it); merge unions both sides.
            val restoredHistory = if (replace) {
                WatchHistoryRules.trim(payload.history ?: emptyList())
            } else {
                WatchHistoryRules.trim(history + (payload.history ?: emptyList()))
            }
            history = restoredHistory
            repo.saveHistory(restoredHistory)
            // Import may have restored streak history (v3 backups).
            _streakDays.value = repo.loadStreakDays()
            _bestStreak.value = repo.loadBestStreak()
            _watchDays.value = repo.loadWatchDays()

            // Reschedule active reminders
            finalItems.forEach { item ->
                if (item.reminderEnabled) {
                    com.seriee.app.reminder.ReminderScheduler.scheduleReminder(getApplication(), item)
                }
            }
            maybeTriggerBackupReminder(finalItems.size, importedDelta = imported.size)
            imported.size
        }
    }
}
