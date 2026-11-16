package com.seriee.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.seriee.app.R
import com.seriee.app.SerieeViewModel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun SerieeApp(vm: SerieeViewModel) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    val nextTabLabel = stringResource(R.string.nav_next_tab)
    val previousTabLabel = stringResource(R.string.nav_previous_tab)

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var showBackupRestore by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showImport by rememberSaveable { mutableStateOf(false) }
    var fabVisible by remember { mutableStateOf(true) }

    // Quick-action triggers handed down to the active screen.
    var searchTick by remember { mutableIntStateOf(0) }
    var comingSoonTick by remember { mutableIntStateOf(0) }

    val pagerState = rememberPagerState(initialPage = tab) { 3 }
    val scope = rememberCoroutineScope()

    val bgBackdrop = rememberLayerBackdrop()
    val chromeBackdrop = rememberLayerBackdrop()

    LaunchedEffect(pagerState.currentPage) {
        tab = pagerState.currentPage
    }
    LaunchedEffect(pagerState.settledPage) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val goToTab: (Int) -> Unit = { index ->
        tab = index
        fabVisible = true
        scope.launch {
            pagerState.animateScrollToPage(
                page = index,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Hide the FAB while the user scrolls down, reveal it on the way back up.
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag) {
                    if (available.y < -6f) fabVisible = false
                    else if (available.y > 6f) fabVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentInsets = PaddingValues(
        top = statusBarTop + 74.dp,
        bottom = navBarBottom + 86.dp
    )

    Box(Modifier.fillMaxSize()) {
        // Captured scene layer (ambient aurora + scrolling pages) refracted by floating Liquid Glass chrome.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(chromeBackdrop)
        ) {
            SerieeBackground(p, modifier = Modifier.layerBackdrop(bgBackdrop))

            CompositionLocalProvider(LocalLiquidBackdrop provides bgBackdrop) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollConnection)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics {
                                    customActions = listOf(
                                        CustomAccessibilityAction(nextTabLabel) {
                                            goToTab((pagerState.currentPage + 1).coerceAtMost(2))
                                            true
                                        },
                                        CustomAccessibilityAction(previousTabLabel) {
                                            goToTab((pagerState.currentPage - 1).coerceAtLeast(0))
                                            true
                                        }
                                    )
                                }
                        ) { page ->
                            Box(Modifier.fillMaxSize()) {
                                when (page) {
                                    0 -> ProgressScreen(
                                        list = vm.items,
                                        padding = contentInsets,
                                        onOpen = { detailId = it.id },
                                        onIncrement = vm::incrementEpisode,
                                        streakDays = vm.streakDays,
                                        bestStreak = vm.bestStreak,
                                        watchDays = vm.watchDays,
                                        onAdd = {
                                            editingId = null
                                            showForm = true
                                        },
                                        searchFocusTick = searchTick,
                                        comingSoonTick = comingSoonTick,
                                        onSearchHandled = { searchTick = 0 },
                                        onComingSoonHandled = { comingSoonTick = 0 }
                                    )
                                    1 -> LibraryScreen(
                                        list = vm.items,
                                        gridView = vm.gridView,
                                        padding = contentInsets,
                                        onSetGrid = vm::setGridView,
                                        onOpen = { detailId = it.id },
                                        onPin = vm::togglePin,
                                        onAdd = {
                                            editingId = null
                                            showForm = true
                                        },
                                        searchFocusTick = searchTick
                                    )
                                    else -> InsightsScreen(
                                        list = vm.items,
                                        history = vm.history,
                                        watchDays = vm.watchDays,
                                        padding = contentInsets,
                                        onOpen = { detailId = it.id },
                                        onAdd = {
                                            editingId = null
                                            showForm = true
                                        },
                                        onClearHistory = vm::clearHistory
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Apple Liquid Glass Chrome (samples live content beneath it)
        CompositionLocalProvider(
            LocalChromeBackdrop provides chromeBackdrop,
            LocalLiquidBackdrop provides chromeBackdrop
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                // Pages scroll under the floating chrome, so fade them out before
                // they reach the clock instead of showing card rows in the status bar.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarTop + 24.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to p.bg.copy(alpha = 0.94f),
                                0.55f to p.bg.copy(alpha = 0.72f),
                                1f to Color.Transparent
                            )
                        )
                )
                SerieeTopBar(
                    vm = vm,
                    chromeBackdrop = chromeBackdrop,
                    onOpenBackupRestore = { showBackupRestore = true },
                    onOpenImport = {
                        vm.beginImport()
                        showImport = true
                    },
                    onOpenChangelog = { vm.openChangelog() },
                    onOpenAbout = { showAbout = true },
                    onSearch = {
                        if (tab != 0) goToTab(0)
                        searchTick++
                    },
                    onSchedule = {
                        goToTab(0)
                        comingSoonTick++
                    }
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .animateContentSize(
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SerieeNavBar(
                    selectedIndex = tab,
                    pagerPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                    chromeBackdrop = chromeBackdrop,
                    onSelect = goToTab,
                    modifier = Modifier.weight(1f)
                )

                AnimatedVisibility(
                    visible = fabVisible && tab != 2,
                    enter = scaleIn(
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(),
                    exit = scaleOut(
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut()
                ) {
                    LiquidAddOrb(
                        chromeBackdrop = chromeBackdrop,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            editingId = null
                            showForm = true
                        }
                    )
                }
            }
        }
    }

    if (showForm) {
        MediaFormDialog(
            initial = editingId?.let { id -> vm.items.find { it.id == id } },
            existing = vm.items,
            onDismiss = { showForm = false },
            onSave = {
                vm.upsert(it)
                showForm = false
            }
        )
    }

    detailId?.let { id ->
        val item = vm.items.find { it.id == id }
        if (item != null) {
            DetailDialog(
                item = item,
                onDismiss = { detailId = null },
                onEdit = {
                    detailId = null
                    editingId = item.id
                    showForm = true
                },
                onDelete = {
                    detailId = null
                    vm.delete(item.id)
                },
                onToggleReminder = { vm.toggleReminder(item.id) },
                onTestReminder = { vm.testReminder(item) },
                onUpdateReleaseDate = { newDate ->
                    vm.upsert(item.copy(releaseDate = newDate, reminderEnabled = true))
                },
                onUpdateRating = { newRating ->
                    vm.upsert(item.copy(rating = newRating))
                },
                onSelectEpisode = { seasonNo, epNo ->
                    vm.setCurrentEpisode(item.id, seasonNo, epNo)
                }
            )
        }
    }

    if (showBackupRestore) {
        BackupRestoreDialog(
            vm = vm,
            onDismiss = { showBackupRestore = false }
        )
    }

    if (showImport) {
        ImportSheet(
            vm = vm,
            onDismiss = { showImport = false },
            onViewLibrary = {
                showImport = false
                goToTab(1)
            }
        )
    }

    if (vm.showChangelog) {
        ChangelogDialog(
            versionName = vm.currentVersionName,
            onDismiss = { vm.dismissChangelog() }
        )
    }

    if (showAbout) {
        AboutDialog(
            versionName = vm.currentVersionName,
            onDismiss = { showAbout = false }
        )
    }
}

@Composable
private fun LiquidAddOrb(
    chromeBackdrop: LayerBackdrop,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fabScale"
    )
    Box(
        modifier = Modifier
            .testTag("fab_add_button")
            .size(60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .liquidGlass(
                shape = CircleShape,
                backdrop = chromeBackdrop,
                tint = p.primary.copy(alpha = if (p.isGlass) 0.68f else 1f),
                blurRadius = 20.dp,
                refractionHeight = 16.dp,
                refractionAmount = 32.dp,
                chromaticAberration = true,
                elevated = true,
                withHighlight = true,
                withBorder = true
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        p.primary.copy(alpha = if (p.isGlass) 0.35f else 1f),
                        p.accent.copy(alpha = if (p.isGlass) 0.35f else 1f)
                    )
                ),
                CircleShape
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = stringResource(R.string.add),
            tint = p.onAccent,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun SerieeTopBar(
    vm: SerieeViewModel,
    chromeBackdrop: LayerBackdrop,
    onOpenBackupRestore: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenAbout: () -> Unit,
    onSearch: () -> Unit,
    onSchedule: () -> Unit
) {
    val p = LocalPalette.current
    var optionsMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .glassChrome(p, RoundedCornerShape(28.dp), backdrop = chromeBackdrop)
            .height(58.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(p.primary, p.accent)))
                    .then(
                        if (p.isGlass) Modifier.border(0.75.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Seriee",
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(p.text, p.accent)),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconPillButton(
                icon = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search),
                onClick = onSearch,
                backdrop = chromeBackdrop
            )
            IconPillButton(
                icon = Icons.Filled.CalendarMonth,
                contentDescription = stringResource(R.string.schedule),
                onClick = onSchedule,
                backdrop = chromeBackdrop
            )
            Box {
                IconPillButton(
                    icon = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    onClick = { optionsMenu = true },
                    backdrop = chromeBackdrop
                )
                SerieeOverflowMenu(
                    vm = vm,
                    expanded = optionsMenu,
                    onDismiss = { optionsMenu = false },
                    onBackupRestore = onOpenBackupRestore,
                    onImport = onOpenImport,
                    onChangelog = onOpenChangelog,
                    onAbout = onOpenAbout,
                    artworkPoster = vm.items.firstOrNull { !it.poster.isNullOrBlank() }?.poster
                )
            }
        }
    }
}

@Composable
private fun SerieeNavBar(
    selectedIndex: Int,
    pagerPosition: Float,
    chromeBackdrop: LayerBackdrop,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    val items = listOf(
        Icons.Filled.Timeline to stringResource(R.string.nav_progress),
        Icons.Filled.VideoLibrary to stringResource(R.string.nav_library),
        Icons.Filled.Insights to stringResource(R.string.nav_insights)
    )

    Box(
        modifier = modifier
            .glassChrome(p, CircleShape, backdrop = chromeBackdrop)
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            val maxPosition = (items.size - 1).toFloat()
            val itemWidth = maxWidth / items.size
            val indicatorWidth = itemWidth - 4.dp
            val targetPosition = pagerPosition.coerceIn(0f, maxPosition)
            val animatedPosition by animateFloatAsState(
                targetValue = targetPosition,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "navPillPosition"
            )
            val position = animatedPosition.coerceIn(0f, maxPosition)
            val normalizedPosition = if (maxPosition == 0f) 0f else position / maxPosition
            val travelVelocity = if (maxPosition == 0f) 0f
            else (abs(targetPosition - animatedPosition) / maxPosition).coerceIn(0f, 1f)
            val glideStretch = (sin(normalizedPosition * PI.toFloat()) * 0.12f + travelVelocity * 0.18f)
                .coerceIn(0f, 0.22f)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = itemWidth * position + 2.dp)
                    .width(indicatorWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = 1f + glideStretch
                        scaleY = 1f - glideStretch * 0.22f
                    }
                    .clip(CircleShape)
                    .background(
                        if (p.isGlass) {
                            Brush.horizontalGradient(
                                listOf(
                                    p.primary.copy(alpha = 0.85f),
                                    p.accent.copy(alpha = 0.85f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(listOf(p.primary, p.accent))
                        }
                    )
                    .then(
                        if (p.isGlass) {
                            Modifier.border(
                                0.75.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.75f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.40f)
                                    )
                                ),
                                CircleShape
                            )
                        } else Modifier
                    )
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { index, (icon, label) ->
                    val selected = abs(position - index) < 0.5f
                    val itemScale by animateFloatAsState(
                        targetValue = if (selected) 1.04f else 0.96f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "navItemScale_$index"
                    )
                    val tag = when (index) {
                        0 -> "nav_tab_progress"
                        1 -> "nav_tab_library"
                        else -> "nav_tab_insights"
                    }
                    Row(
                        modifier = Modifier
                            .testTag(tag)
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelect(index)
                            }
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (selected) p.onAccent else p.text.copy(alpha = 0.78f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (selected) p.onAccent else p.text.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }
    }
}
