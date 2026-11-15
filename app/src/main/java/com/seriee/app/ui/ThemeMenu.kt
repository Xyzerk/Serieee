package com.seriee.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seriee.app.R
import com.seriee.app.SerieeViewModel
import com.seriee.app.data.AppLanguage
import com.seriee.app.data.AppThemeMode
import com.seriee.app.data.AppThemeStyle
import kotlinx.coroutines.launch

private enum class MenuPage { Root, Theme, Customize, Language }

/** Accent presets offered by the custom theme. */
private val accentPresets = listOf(
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
private val gradientPresets = listOf(
    Color(0xFF1D4ED8) to Color(0xFF7C2A64),
    Color(0xFF0EA5E9) to Color(0xFF6366F1),
    Color(0xFFF0559B) to Color(0xFF8B5CF6),
    Color(0xFF10B981) to Color(0xFF06B6D4),
    Color(0xFFF59E0B) to Color(0xFFEF4444),
    Color(0xFF64748B) to Color(0xFF0F172A)
)

/**
 * Root overflow menu with the Theme → Customize page stack.
 *
 * Back (system or in-menu arrow) walks up the stack before closing the popup.
 */
@Composable
fun SerieeOverflowMenu(
    vm: SerieeViewModel,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onBackupRestore: () -> Unit,
    onImport: () -> Unit,
    onChangelog: () -> Unit,
    onAbout: () -> Unit,
    artworkPoster: String?
) {
    val p = LocalPalette.current
    val systemDark = isSystemInDarkTheme()
    var page by remember { mutableStateOf(MenuPage.Root) }
    val maxHeight = minOf(
        560.dp,
        LocalConfiguration.current.screenHeightDp.dp * 0.72f
    )

    LaunchedEffect(expanded) {
        if (expanded) page = MenuPage.Root
    }

    BackHandler(enabled = expanded) {
        if (page == MenuPage.Root) onDismiss() else page = MenuPage.Root
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(312.dp)
    ) {
        AnimatedContent(
            targetState = page,
            label = "menuPage",
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                }
            }
        ) { current ->
            when (current) {
                MenuPage.Root -> Column {
                    SettingRow(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.theme),
                        value = "${stringResource(vm.themeMode.labelRes)} · ${stringResource(vm.themeStyle.labelRes)}",
                        tint = p.accent
                    ) { page = MenuPage.Theme }
                    SettingRow(
                        icon = Icons.Filled.Translate,
                        title = stringResource(R.string.language),
                        value = stringResource(vm.language.labelRes)
                    ) { page = MenuPage.Language }
                    HorizontalDivider(color = p.border)
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
                    HorizontalDivider(color = p.border)
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
                        Icon(Icons.Filled.Campaign, null, tint = p.muted, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.changelog_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = p.text,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(p.accent)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                stringResource(R.string.new_badge),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.onAccent
                            )
                        }
                    }
                    SettingRow(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.about_title)
                    ) {
                        onDismiss()
                        onAbout()
                    }
                }

                MenuPage.Theme -> ThemePage(
                    vm = vm,
                    systemDark = systemDark,
                    maxHeight = maxHeight,
                    onBack = { page = MenuPage.Root },
                    onCustomize = { page = MenuPage.Customize }
                )

                MenuPage.Customize -> CustomizePage(
                    vm = vm,
                    systemDark = systemDark,
                    maxHeight = maxHeight,
                    artworkPoster = artworkPoster,
                    onBack = { page = MenuPage.Theme }
                )

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
private fun MenuHeader(title: String, onBack: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Spacer(Modifier.width(6.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.text)
    }
    HorizontalDivider(color = p.border)
}

@Composable
private fun SectionLabel(text: String) {
    val p = LocalPalette.current
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = p.muted,
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
        gradient = vm.customGradient
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .background(if (isSelected) p.accentSoft else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton
                    ) { vm.setThemeStyle(style) }
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
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) p.accent else p.text
                    )
                    Text(
                        stringResource(style.descriptionRes),
                        fontSize = 10.sp,
                        color = p.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = p.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = p.border, modifier = Modifier.padding(top = 6.dp))
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
        HorizontalDivider(color = p.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onCustomize)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Palette, contentDescription = null, tint = p.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.theme_customize),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = p.accent
            )
        }
    }
}

@Composable
private fun SwitchRow(
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
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = p.text)
            Text(subtitle, fontSize = 10.sp, color = p.muted, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = p.onAccent,
                checkedTrackColor = p.accent,
                uncheckedThumbColor = p.muted,
                uncheckedTrackColor = p.chip,
                uncheckedBorderColor = p.border
            )
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
private fun CustomizePage(
    vm: SerieeViewModel,
    systemDark: Boolean,
    maxHeight: androidx.compose.ui.unit.Dp,
    artworkPoster: String?,
    onBack: () -> Unit
) {
    val p = LocalPalette.current
    val custom = CustomThemeColors(
        accent = vm.customAccent,
        backgroundStart = vm.customBackgroundStart,
        backgroundEnd = vm.customBackgroundEnd,
        gradient = vm.customGradient
    )
    val live = resolvePalette(
        mode = vm.themeMode,
        style = AppThemeStyle.Custom,
        systemDark = systemDark,
        custom = custom,
        reduceTransparency = vm.reduceTransparency
    )
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var pickingArtwork by remember { mutableStateOf(false) }
    var codeMessage by remember { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
    ) {
        MenuHeader(stringResource(R.string.customize_theme_title), onBack)

        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.accent),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = p.muted
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                accentPresets.forEach { preset ->
                    val selected = preset.value == vm.customAccent.value
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(preset)
                            .border(
                                2.dp,
                                if (selected) p.text else Color.Transparent,
                                CircleShape
                            )
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton
                            ) { vm.setCustomAccent(preset) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = if (preset.luminance() > 0.6f) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (artworkPoster != null) p.chip else p.chip.copy(alpha = 0.4f))
                    .clickable(enabled = artworkPoster != null && !pickingArtwork) {
                        pickingArtwork = true
                        scope.launch {
                            val color = artworkPoster?.let { accentFromPoster(context, it) }
                            if (color != null) vm.setCustomAccent(color)
                            pickingArtwork = false
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pickingArtwork) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = p.accent
                    )
                } else {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = if (artworkPoster != null) p.accent else p.muted,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.theme_accent_from_artwork),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (artworkPoster != null) p.text else p.muted
                )
            }
            if (!live.hasReadableAccent()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = p.warning,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.theme_accent_low_contrast),
                        fontSize = 10.sp,
                        color = p.warning
                    )
                }
            }
        }

        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.background),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = p.muted
            )
            SegmentedTabs(
                labels = listOf(
                    stringResource(R.string.background_solid),
                    stringResource(R.string.background_gradient)
                ),
                selected = if (vm.customGradient) 1 else 0,
                onSelect = { vm.setCustomGradient(it == 1) }
            )
            if (vm.customGradient) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gradientPresets.forEach { (start, end) ->
                        val selected = start.value == vm.customBackgroundStart.value &&
                            end.value == vm.customBackgroundEnd.value
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Brush.linearGradient(listOf(start, end)))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) p.text else p.border,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton
                                ) { vm.setCustomBackground(start, end) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = p.border, modifier = Modifier.padding(top = 8.dp))
        CodeRow(
            icon = Icons.Filled.ContentCopy,
            label = stringResource(R.string.theme_copy_code)
        ) {
            clipboard.setText(AnnotatedString(vm.themeCode()))
            codeMessage = R.string.theme_code_copied
        }
        CodeRow(
            icon = Icons.Filled.ContentPaste,
            label = stringResource(R.string.theme_paste_code)
        ) {
            val applied = vm.applyThemeCode(clipboard.getText()?.text)
            codeMessage = if (applied) R.string.theme_code_applied else R.string.theme_code_invalid
        }
        codeMessage?.let { message ->
            Text(
                text = stringResource(message),
                fontSize = 10.sp,
                color = p.muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
        HorizontalDivider(color = p.border)

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.theme_preview),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = p.muted
            )
            ThemePreview(live)
        }
    }
}

@Composable
private fun CodeRow(
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
        Icon(icon, contentDescription = null, tint = p.muted, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = p.text)
    }
}

@Composable
private fun ThemePreview(palette: SeriePalette) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                palette.gradient?.let { Brush.linearGradient(it) }
                    ?: Brush.linearGradient(listOf(palette.bg, palette.bg))
            )
            .border(1.dp, palette.border, shape)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(palette.primary)
                )
                Spacer(Modifier.width(8.dp))
                Text("Seriee", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = palette.text)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.accent.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        stringResource(R.string.theme_preview_chip),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (palette.isGlass) palette.card.copy(alpha = 0.75f) else palette.card)
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
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
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(palette.chip)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.62f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(if (isSelected) p.accentSoft else Color.Transparent)
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
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) p.accent else p.text,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = p.accent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
