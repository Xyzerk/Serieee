package com.seriee.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seriee.app.R
import com.seriee.app.debug.AppLogger
import com.seriee.app.SerieeViewModel
import com.seriee.app.data.AppLanguage
import com.seriee.app.data.AppThemeMode
import com.seriee.app.data.AppThemeStyle
import kotlinx.coroutines.launch

private enum class MenuPage { Root, Language }

/** Accent presets offered by the custom theme. */
internal val accentPresets = listOf(
    Color(0xFF3B82F6),
    Color(0xFFF0559B),
    Color(0xFF8B5CF6),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
    Color(0xFF06B6D4),
    Color(0xFFF8FAFC)
)

/** Two-stop background gradients for the custom theme. */
internal val gradientPresets = listOf(
    Color(0xFF1D4ED8) to Color(0xFF7C2A64),
    Color(0xFF0EA5E9) to Color(0xFF6366F1),
    Color(0xFFF0559B) to Color(0xFF8B5CF6),
    Color(0xFF10B981) to Color(0xFF06B6D4),
    Color(0xFFF59E0B) to Color(0xFFEF4444),
    Color(0xFF64748B) to Color(0xFF0F172A)
)

/** Localised TalkBack name for each Neo-Brutalism tone swatch. */
private val NeoTone.labelRes: Int
    get() = when (this) {
        NeoTone.Yellow -> R.string.theme_tone_yellow
        NeoTone.Orange -> R.string.theme_tone_orange
        NeoTone.Pink -> R.string.theme_tone_pink
        NeoTone.Purple -> R.string.theme_tone_purple
        NeoTone.Blue -> R.string.theme_tone_blue
        NeoTone.Mint -> R.string.theme_tone_mint
    }

/**
 * Theme popup opened from its own top-bar button. [onOpenStudio] launches the
 * full-screen Theme Studio for deep customisation.
 */
@Composable
fun SerieeThemeMenu(
    vm: SerieeViewModel,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpenStudio: () -> Unit
) {
    val p = LocalPalette.current
    val systemDark = isSystemInDarkTheme()
    val maxHeight = minOf(
        560.dp,
        LocalConfiguration.current.screenHeightDp.dp * 0.72f
    ).coerceAtLeast(120.dp)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = if (p.isNeoBrutalist) RoundedCornerShape(8.dp) else RoundedCornerShape(24.dp),
        containerColor = if (p.isNeoBrutalist) p.surface else if (p.isGlass) p.elevated else p.surface,
        tonalElevation = 0.dp,
        shadowElevation = if (p.isNeoBrutalist) 0.dp else 8.dp,
        border = if (p.isNeoBrutalist) BorderStroke(2.5.dp, p.border) else null,
        modifier = Modifier.width(312.dp)
    ) {
        ThemePage(
            vm = vm,
            systemDark = systemDark,
            maxHeight = maxHeight,
            onBack = onDismiss,
            onCustomize = {
                onDismiss()
                onOpenStudio()
            }
        )
    }
}

/**
 * Root overflow menu with the Language page.
 *
 * Back (system or in-menu arrow) returns to the root page before closing the popup.
 */
@Composable
fun SerieeOverflowMenu(
    vm: SerieeViewModel,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onBackupRestore: () -> Unit,
    onApiKeys: () -> Unit,
    onImport: () -> Unit,
    onChangelog: () -> Unit,
    onAbout: () -> Unit
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    var page by remember { mutableStateOf(MenuPage.Root) }

    LaunchedEffect(expanded) {
        if (expanded) page = MenuPage.Root
    }

    BackHandler(enabled = expanded) {
        if (page == MenuPage.Root) onDismiss() else page = MenuPage.Root
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = if (p.isNeoBrutalist) RoundedCornerShape(8.dp) else RoundedCornerShape(24.dp),
        containerColor = if (p.isNeoBrutalist) p.surface else if (p.isGlass) p.elevated else p.surface,
        tonalElevation = 0.dp,
        shadowElevation = if (p.isNeoBrutalist) 0.dp else 8.dp,
        border = if (p.isNeoBrutalist) BorderStroke(2.5.dp, p.border) else null,
        modifier = Modifier.width(312.dp)
    ) {
        AnimatedContent(
            targetState = page,
            label = "menuPage",
            transitionSpec = {
                val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(
                    durationMillis = 220,
                    easing = SerieeMotion.EaseOutExpo
                )
                val fadeInSpec = tween<Float>(durationMillis = 190, easing = SerieeMotion.StandardEasing)
                val fadeOutSpec = tween<Float>(durationMillis = 130, easing = SerieeMotion.StandardEasing)
                val transition = if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally(slideSpec) { it / 4 } + fadeIn(fadeInSpec)) togetherWith
                        (slideOutHorizontally(slideSpec) { -it / 4 } + fadeOut(fadeOutSpec))
                } else {
                    (slideInHorizontally(slideSpec) { -it / 4 } + fadeIn(fadeInSpec)) togetherWith
                        (slideOutHorizontally(slideSpec) { it / 4 } + fadeOut(fadeOutSpec))
                }
                transition using SizeTransform(
                    clip = true,
                    sizeAnimationSpec = { _, _ ->
                        tween(durationMillis = 220, easing = SerieeMotion.EaseOutExpo)
                    }
                )
            }
        ) { current ->
            when (current) {
                MenuPage.Root -> Column {
                    SettingRow(
                        icon = Icons.Filled.Translate,
                        title = stringResource(R.string.language),
                        value = stringResource(vm.language.labelRes)
                    ) { page = MenuPage.Language }
                    HorizontalDivider(color = p.border, thickness = if (p.isNeoBrutalist) 2.dp else 1.dp)
                    SettingRow(
                        icon = Icons.Filled.CloudDownload,
                        title = stringResource(R.string.import_collection)
                    ) {
                        onDismiss()
                        onImport()
                    }
                    SettingRow(
                        icon = Icons.Filled.Storage,
                        title = stringResource(R.string.backup_restore)
                    ) {
                        onDismiss()
                        onBackupRestore()
                    }
                    SettingRow(
                        icon = Icons.Filled.Key,
                        title = stringResource(R.string.api_keys),
                        tint = if (p.isNeoBrutalist) p.text else p.primary
                    ) {
                        onDismiss()
                        onApiKeys()
                    }
                    HorizontalDivider(color = p.border, thickness = if (p.isNeoBrutalist) 2.dp else 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                onDismiss()
                                onChangelog()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Campaign, null, tint = if (p.isNeoBrutalist) p.text else p.muted, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.changelog_title),
                            fontSize = 14.sp,
                            fontWeight = if (p.isNeoBrutalist) FontWeight.Bold else FontWeight.Medium,
                            color = p.text,
                            modifier = Modifier.weight(1f)
                        )
                        val badgeShape = RoundedCornerShape(if (p.isNeoBrutalist) 6.dp else 5.dp)
                        Box(
                            Modifier
                                .clip(badgeShape)
                                .background(if (p.isNeoBrutalist) NeoBrutalistColors.Yellow else p.accent)
                                .then(
                                    if (p.isNeoBrutalist) Modifier.border(1.5.dp, NeoBrutalistColors.Black, badgeShape)
                                    else Modifier
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                stringResource(R.string.new_badge),
                                fontFamily = if (p.isNeoBrutalist) FontFamily.Monospace else FontFamily.Default,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (p.isNeoBrutalist) NeoBrutalistColors.Black else p.onAccent
                            )
                        }
                    }
                    SettingRow(
                        icon = Icons.Filled.BugReport,
                        title = stringResource(R.string.report_bug)
                    ) {
                        onDismiss()
                        AppLogger.shareBugReport(context)
                    }
                    SettingRow(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.about_title)
                    ) {
                        onDismiss()
                        onAbout()
                    }
                }

                MenuPage.Language -> LanguagePage(
                    selected = vm.language,
                    onBack = { page = MenuPage.Root },
                    onLanguage = vm::setLanguage
                )
            }
        }
    }
}

@Composable
internal fun MenuHeader(title: String, onBack: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (p.isNeoBrutalist) {
            NeoBrutalistBox(
                modifier = Modifier.size(38.dp),
                onClick = onBack,
                role = Role.Button,
                backgroundColor = NeoBrutalistColors.OffWhite,
                borderColor = p.border,
                shadowColor = NeoBrutalistColors.Black,
                borderWidth = 2.dp,
                shadowOffset = 2.dp,
                cornerRadius = 6.dp,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = NeoBrutalistColors.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = p.text,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = if (p.isNeoBrutalist) FontWeight.ExtraBold else FontWeight.Bold,
            color = p.text
        )
    }
    HorizontalDivider(color = p.border, thickness = if (p.isNeoBrutalist) 2.dp else 1.dp)
}

@Composable
internal fun SectionLabel(text: String) {
    val p = LocalPalette.current
    Text(
        text = if (p.isNeoBrutalist) text.uppercase() else text,
        fontFamily = if (p.isNeoBrutalist) FontFamily.Monospace else FontFamily.Default,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (p.isNeoBrutalist) p.text else p.muted,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun ThemePage(
    vm: SerieeViewModel,
    systemDark: Boolean,
    maxHeight: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onCustomize: () -> Unit
) {
    val p = LocalPalette.current
    val custom = CustomThemeColors(
        accent = vm.customAccent,
        backgroundStart = vm.customBackgroundStart,
        backgroundEnd = vm.customBackgroundEnd,
        gradient = vm.customGradient,
        glass = vm.customGlass,
        highContrast = vm.customHighContrast,
        extras = vm.customExtras
    )
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
    ) {
        MenuHeader(stringResource(R.string.theme), onBack)

        SectionLabel(stringResource(R.string.theme_appearance))
        Box(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            SegmentedTabs(
                labels = AppThemeMode.entries.map { stringResource(it.labelRes) },
                selected = vm.themeMode.ordinal,
                onSelect = { index -> vm.setThemeMode(AppThemeMode.entries[index]) }
            )
        }

        SectionLabel(stringResource(R.string.theme_style))
        AppThemeStyle.entries.forEach { style ->
            val isSelected = style == vm.themeStyle
            val selectedBg by animateColorAsState(
                targetValue = when {
                    !isSelected -> Color.Transparent
                    p.isNeoBrutalist -> NeoBrutalistColors.Yellow
                    else -> p.accentSoft
                },
                animationSpec = tween(SerieeMotion.quick, easing = SerieeMotion.StandardEasing),
                label = "themeRowBg_${style.name}"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .background(selectedBg)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton
                    ) {
                        vm.setThemeStyle(style)
                        // Custom is an editor, not a dead end: selecting it opens
                        // the colour/surface controls straight away.
                        if (style == AppThemeStyle.Custom) onCustomize()
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeSwatch(
                    mode = vm.themeMode,
                    style = style,
                    systemDark = systemDark,
                    custom = custom
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(style.labelRes),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected || p.isNeoBrutalist) FontWeight.ExtraBold else FontWeight.Medium,
                        color = when {
                            p.isNeoBrutalist && isSelected -> NeoBrutalistColors.Black
                            p.isNeoBrutalist -> p.text
                            isSelected -> p.accent
                            else -> p.text
                        }
                    )
                    Text(
                        stringResource(style.descriptionRes),
                        fontSize = 10.sp,
                        color = if (p.isNeoBrutalist && isSelected) NeoBrutalistColors.Black.copy(alpha = 0.78f) else p.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (p.isNeoBrutalist) NeoBrutalistColors.Black else p.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (style == AppThemeStyle.NeoBrutalism && isSelected) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 58.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        stringResource(R.string.theme_neo_tone),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = p.text
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeoTone.entries.forEach { tone ->
                            val toneSelected = tone == vm.neoTone
                            val toneLabel = stringResource(tone.labelRes)
                            val toneShape = RoundedCornerShape(6.dp)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .neoBrutalist(
                                        backgroundColor = tone.color,
                                        borderColor = NeoBrutalistColors.Black,
                                        shadowColor = NeoBrutalistColors.Black,
                                        borderWidth = if (toneSelected) 3.dp else 2.dp,
                                        shadowOffset = if (toneSelected) 0.dp else 2.dp,
                                        cornerRadius = 6.dp
                                    )
                                    .clip(toneShape)
                                    // Unselected swatches have no visible text, so without
                                    // this TalkBack announces a nameless radio button.
                                    .semantics { contentDescription = toneLabel }
                                    .selectable(
                                        selected = toneSelected,
                                        role = Role.RadioButton
                                    ) { vm.setNeoTone(tone) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (toneSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = NeoBrutalistColors.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (style == AppThemeStyle.Custom) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                        .clickable(onClick = onCustomize)
                        .padding(start = 58.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Palette,
                        contentDescription = null,
                        tint = if (p.isNeoBrutalist) p.text else p.accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.theme_customize),
                        fontSize = 12.sp,
                        fontWeight = if (p.isNeoBrutalist) FontWeight.Bold else FontWeight.Medium,
                        color = if (p.isNeoBrutalist) p.text else p.accent
                    )
                }
            }
        }

        HorizontalDivider(
            color = p.border,
            thickness = if (p.isNeoBrutalist) 2.dp else 1.dp,
            modifier = Modifier.padding(top = 6.dp)
        )
        SwitchRow(
            title = stringResource(R.string.theme_reduce_transparency),
            subtitle = stringResource(R.string.theme_reduce_transparency_desc),
            checked = vm.reduceTransparency,
            onCheckedChange = vm::setReduceTransparency
        )
        SwitchRow(
            title = stringResource(R.string.theme_battery_amoled),
            subtitle = stringResource(R.string.theme_battery_amoled_desc),
            checked = vm.batterySaverAmoled,
            onCheckedChange = vm::setBatterySaverAmoled
        )
    }
}

@Composable
internal fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = if (p.isNeoBrutalist) FontWeight.Bold else FontWeight.Medium,
                color = p.text
            )
            Text(subtitle, fontSize = 10.sp, color = p.muted, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = if (p.isNeoBrutalist) {
                SwitchDefaults.colors(
                    checkedThumbColor = NeoBrutalistColors.Black,
                    checkedTrackColor = NeoBrutalistColors.Yellow,
                    checkedBorderColor = NeoBrutalistColors.Black,
                    uncheckedThumbColor = NeoBrutalistColors.Black,
                    uncheckedTrackColor = p.chip,
                    uncheckedBorderColor = p.border
                )
            } else {
                SwitchDefaults.colors(
                    checkedThumbColor = p.onAccent,
                    checkedTrackColor = p.accent,
                    uncheckedThumbColor = p.muted,
                    uncheckedTrackColor = p.chip,
                    uncheckedBorderColor = p.border
                )
            }
        )
    }
}

@Composable
private fun ThemeSwatch(
    mode: AppThemeMode,
    style: AppThemeStyle,
    systemDark: Boolean,
    custom: CustomThemeColors
) {
    val dark = when (mode) {
        AppThemeMode.System -> systemDark
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }
    val dynamicScheme = rememberMaterialYouScheme(style == AppThemeStyle.MaterialYou, dark)
    val palette = previewPalette(mode, style, systemDark, custom, dynamicScheme)
    if (style == AppThemeStyle.NeoBrutalism) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .neoBrutalist(
                    backgroundColor = palette.bg,
                    borderColor = NeoBrutalistColors.Black,
                    shadowColor = NeoBrutalistColors.Black,
                    borderWidth = 2.dp,
                    shadowOffset = 2.dp,
                    cornerRadius = 6.dp
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(NeoBrutalistColors.Yellow)
                    .border(1.5.dp, NeoBrutalistColors.Black, RoundedCornerShape(3.dp))
            )
        }
        return
    }
    val brush = when {
        mode == AppThemeMode.System -> Brush.linearGradient(
            listOf(
                previewPalette(AppThemeMode.Light, style, false, custom, dynamicScheme).bg,
                previewPalette(AppThemeMode.Dark, style, true, custom, dynamicScheme).bg
            )
        )
        palette.gradient != null -> Brush.linearGradient(palette.gradient)
        else -> Brush.linearGradient(listOf(palette.bg, palette.card))
    }
    val outline = if (palette.isLight) palette.text.copy(alpha = 0.28f) else palette.border
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(brush)
            .border(1.dp, outline, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(palette.accent)
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CodeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (p.isNeoBrutalist) p.text else p.muted, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (p.isNeoBrutalist) FontWeight.Bold else FontWeight.Medium,
            color = p.text
        )
    }
}

@Composable
internal fun HexColorField(
    label: String,
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val p = LocalPalette.current
    val canonical = color.toHexRgb()
    // Keyed on the applied colour so preset picks refresh the text but partial
    // typing is never overwritten mid-edit.
    var text by remember(canonical) { mutableStateOf(canonical) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            fontSize = 10.sp,
            fontFamily = if (p.isNeoBrutalist) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.Bold,
            color = p.muted
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val swatchShape = if (p.isNeoBrutalist) RoundedCornerShape(6.dp) else RoundedCornerShape(9.dp)
            Box(
                Modifier
                    .size(width = 44.dp, height = 34.dp)
                    .clip(swatchShape)
                    .background(color)
                    .border(
                        if (p.isNeoBrutalist) 2.dp else 1.dp,
                        if (p.isNeoBrutalist) NeoBrutalistColors.Black else p.border,
                        swatchShape
                    )
            )
            SField(
                value = text,
                onChange = { raw ->
                    val clean = raw.uppercase()
                        .filter { it in '0'..'9' || it in 'A'..'F' }
                        .take(6)
                    text = clean
                    if (clean.length == 6) {
                        clean.toLongOrNull(16)?.let { value ->
                            onColorChange(Color(0xFF000000L or value))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = stringResource(R.string.theme_hex_placeholder),
                keyboardType = KeyboardType.Ascii
            )
        }
    }
}

internal fun Color.toHexRgb(): String =
    (toArgb() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

@Composable
internal fun ThemePreview(palette: SeriePalette) {
    val p = LocalPalette.current
    val useNeo = p.isNeoBrutalist || palette.isNeoBrutalist
    val shape = RoundedCornerShape(if (useNeo) 8.dp else 16.dp)
    val previewModifier = if (useNeo) {
        Modifier
            .fillMaxWidth()
            .neoBrutalist(
                backgroundColor = palette.bg,
                borderColor = NeoBrutalistColors.Black,
                shadowColor = NeoBrutalistColors.Black,
                borderWidth = 2.dp,
                shadowOffset = 3.dp,
                cornerRadius = 8.dp
            )
            .padding(12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                palette.gradient?.let { Brush.linearGradient(it) }
                    ?: Brush.linearGradient(listOf(palette.bg, palette.bg))
            )
            .border(1.dp, palette.border, shape)
            .padding(12.dp)
    }
    Box(modifier = previewModifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(if (useNeo) 6.dp else 7.dp))
                        .background(palette.primary)
                        .then(
                            if (useNeo) {
                                Modifier.border(2.dp, NeoBrutalistColors.Black, RoundedCornerShape(6.dp))
                            } else {
                                Modifier
                            }
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text("Seriee", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = palette.text)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (useNeo) NeoBrutalistColors.SkyBlue else palette.accent.copy(alpha = 0.2f))
                        .then(
                            if (useNeo) {
                                Modifier.border(1.5.dp, NeoBrutalistColors.Black, RoundedCornerShape(6.dp))
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        stringResource(R.string.theme_preview_chip),
                        fontSize = 8.sp,
                        fontFamily = if (useNeo) FontFamily.Monospace else FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        color = if (useNeo) NeoBrutalistColors.Black else palette.accent
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(if (useNeo) 8.dp else 12.dp))
                    .background(if (palette.isGlass) palette.card.copy(alpha = 0.75f) else palette.card)
                    .border(
                        if (useNeo) 2.dp else 1.dp,
                        if (useNeo) NeoBrutalistColors.Black else palette.border,
                        RoundedCornerShape(if (useNeo) 8.dp else 12.dp)
                    )
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.theme_preview_card_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.text
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(if (useNeo) 8.dp else 5.dp)
                            .clip(RoundedCornerShape(if (useNeo) 4.dp else 50.dp))
                            .background(palette.chip)
                            .then(
                                if (useNeo) {
                                    Modifier.border(1.5.dp, NeoBrutalistColors.Black, RoundedCornerShape(4.dp))
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.62f)
                                .height(if (useNeo) 8.dp else 5.dp)
                                .clip(RoundedCornerShape(if (useNeo) 4.dp else 50.dp))
                                .background(palette.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePage(
    selected: AppLanguage,
    onBack: () -> Unit,
    onLanguage: (AppLanguage) -> Unit
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        MenuHeader(stringResource(R.string.language), onBack)
        AppLanguage.entries.forEach { option ->
            val isSelected = option == selected
            val rowBg by animateColorAsState(
                targetValue = when {
                    isSelected && p.isNeoBrutalist -> NeoBrutalistColors.Yellow
                    isSelected -> p.accentSoft
                    else -> Color.Transparent
                },
                animationSpec = tween(SerieeMotion.quick, easing = SerieeMotion.StandardEasing),
                label = "langBg_${option.name}"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(rowBg)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton
                    ) { onLanguage(option) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(option.labelRes),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected || p.isNeoBrutalist) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected && p.isNeoBrutalist -> NeoBrutalistColors.Black
                        isSelected -> p.accent
                        else -> p.text
                    },
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (p.isNeoBrutalist) NeoBrutalistColors.Black else p.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
