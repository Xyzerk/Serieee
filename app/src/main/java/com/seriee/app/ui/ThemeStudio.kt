package com.seriee.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.seriee.app.R
import com.seriee.app.SerieeViewModel
import com.seriee.app.data.AppThemeStyle
import com.seriee.app.data.CustomThemeExtras
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

/** One editable colour token of the custom theme. */
private class ColorToken(
    val key: String,
    val labelRes: Int,
    val read: (CustomThemeExtras) -> Long?,
    val write: (CustomThemeExtras, Long?) -> CustomThemeExtras,
    val current: (SeriePalette) -> Color
)

private val colorTokens = listOf(
    ColorToken("text", R.string.studio_c_text, { it.text }, { x, v -> x.copy(text = v) }, { it.text }),
    ColorToken("muted", R.string.studio_c_muted, { it.muted }, { x, v -> x.copy(muted = v) }, { it.muted }),
    ColorToken("card", R.string.studio_c_card, { it.card }, { x, v -> x.copy(card = v) }, { it.card }),
    ColorToken("surface", R.string.studio_c_surface, { it.surface }, { x, v -> x.copy(surface = v) }, { it.surface }),
    ColorToken("border", R.string.studio_c_border, { it.border }, { x, v -> x.copy(border = v) }, { it.border }),
    ColorToken("success", R.string.studio_c_success, { it.success }, { x, v -> x.copy(success = v) }, { it.success }),
    ColorToken("warning", R.string.studio_c_warning, { it.warning }, { x, v -> x.copy(warning = v) }, { it.warning }),
    ColorToken("danger", R.string.studio_c_danger, { it.danger }, { x, v -> x.copy(danger = v) }, { it.danger }),
    ColorToken("info", R.string.studio_c_info, { it.info }, { x, v -> x.copy(info = v) }, { it.info }),
    ColorToken("reminder", R.string.studio_c_reminder, { it.reminder }, { x, v -> x.copy(reminder = v) }, { it.reminder }),
    ColorToken("star", R.string.studio_c_star, { it.star }, { x, v -> x.copy(star = v) }, { it.star })
)

private fun hueColor(hue: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue.coerceIn(0f, 360f), 1f, 1f)))

private fun Color.toLongArgb(): Long = toArgb().toLong() and 0xFFFFFFFFL

/**
 * Full-screen theme editor. Every change is applied live to the whole app, which
 * is visible through the editor itself because it is drawn with the same theme.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeStudioDialog(
    vm: SerieeViewModel,
    artworkPoster: String?,
    onDismiss: () -> Unit
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val extras = vm.customExtras

    val live = resolvePalette(
        mode = vm.themeMode,
        style = AppThemeStyle.Custom,
        systemDark = systemDark,
        custom = CustomThemeColors(
            accent = vm.customAccent,
            backgroundStart = vm.customBackgroundStart,
            backgroundEnd = vm.customBackgroundEnd,
            gradient = vm.customGradient,
            glass = vm.customGlass,
            highContrast = vm.customHighContrast,
            extras = extras
        ),
        reduceTransparency = vm.reduceTransparency
    )

    var expandedKey by remember { mutableStateOf<String?>(null) }
    var pickingArtwork by remember { mutableStateOf(false) }
    var codeMessage by remember { mutableStateOf<Int?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.setCustomBackgroundImage(context, uri) }
    val launchPicker = {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            SerieeBackground(p)
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                StudioHeader(onClose = onDismiss)
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        stringResource(R.string.studio_hint),
                        fontSize = 12.sp,
                        color = p.muted
                    )

                    // ---------------------------------------------------- preview
                    StudioCard(title = stringResource(R.string.theme_preview)) {
                        ThemePreview(live)
                    }

                    // -------------------------------------------------- background
                    StudioCard(title = stringResource(R.string.background)) {
                        val selectedTab = when {
                            extras.hasImage -> 2
                            vm.customGradient -> 1
                            else -> 0
                        }
                        SegmentedTabs(
                            labels = listOf(
                                stringResource(R.string.background_solid),
                                stringResource(R.string.background_gradient),
                                stringResource(R.string.studio_bg_image)
                            ),
                            selected = selectedTab,
                            onSelect = { index ->
                                when (index) {
                                    0 -> {
                                        vm.updateCustomExtras { it.copy(imageEnabled = false) }
                                        vm.setCustomGradient(false)
                                    }
                                    1 -> {
                                        vm.updateCustomExtras { it.copy(imageEnabled = false) }
                                        vm.setCustomGradient(true)
                                    }
                                    else -> if (extras.imagePath != null) {
                                        vm.updateCustomExtras { it.copy(imageEnabled = true) }
                                    } else launchPicker()
                                }
                            }
                        )

                        if (selectedTab == 2) {
                            ImageBackgroundControls(
                                extras = extras,
                                onPick = launchPicker,
                                onRemove = vm::removeCustomBackgroundImage,
                                onChange = { transform -> vm.updateCustomExtras(transform) }
                            )
                        }

                        ColorRow(
                            id = "bg1",
                            label = stringResource(
                                when {
                                    selectedTab == 1 -> R.string.theme_hex_start
                                    selectedTab == 2 -> R.string.studio_base_color
                                    else -> R.string.theme_hex_background
                                }
                            ),
                            color = vm.customBackgroundStart,
                            overridden = false,
                            expandedKey = expandedKey,
                            onExpand = { expandedKey = it },
                            onChange = { vm.setCustomBackground(it, vm.customBackgroundEnd) },
                            onReset = null
                        )
                        if (selectedTab == 1) {
                            ColorRow(
                                id = "bg2",
                                label = stringResource(R.string.theme_hex_end),
                                color = vm.customBackgroundEnd,
                                overridden = false,
                                expandedKey = expandedKey,
                                onExpand = { expandedKey = it },
                                onChange = { vm.setCustomBackground(vm.customBackgroundStart, it) },
                                onReset = null
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                gradientPresets.forEach { preset ->
                                    val start = preset.start
                                    val end = preset.end
                                    val label = stringResource(preset.labelRes)
                                    val selected = start.value == vm.customBackgroundStart.value &&
                                        end.value == vm.customBackgroundEnd.value
                                    val shape = RoundedCornerShape(if (p.isNeoBrutalist) 6.dp else 9.dp)
                                    Box(
                                        modifier = Modifier
                                            .size(width = 52.dp, height = 30.dp)
                                            .clip(shape)
                                            .background(Brush.linearGradient(listOf(start, end)))
                                            .border(
                                                if (selected) 2.5.dp else 1.dp,
                                                if (selected) p.text else p.border,
                                                shape
                                            )
                                            .semantics { contentDescription = label }
                                            .selectable(selected = selected, role = Role.RadioButton) {
                                                vm.setCustomGradient(true)
                                                vm.setCustomBackground(start, end)
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // ------------------------------------------------------ accent
                    StudioCard(title = stringResource(R.string.accent)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            accentPresets.forEach { preset ->
                                val label = stringResource(preset.labelRes)
                                val selected = preset.color.value == vm.customAccent.value
                                val shape = if (p.isNeoBrutalist) RoundedCornerShape(6.dp) else CircleShape
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(shape)
                                        .background(preset.color)
                                        .border(
                                            2.dp,
                                            when {
                                                selected -> p.text
                                                p.isNeoBrutalist -> p.border
                                                else -> Color.Transparent
                                            },
                                            shape
                                        )
                                        .semantics { contentDescription = label }
                                        .selectable(selected = selected, role = Role.RadioButton) {
                                            vm.setCustomAccent(preset.color)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = if (preset.color.luminance() > 0.6f) Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        ColorRow(
                            id = "accent",
                            label = stringResource(R.string.theme_hex_accent),
                            color = vm.customAccent,
                            overridden = false,
                            expandedKey = expandedKey,
                            onExpand = { expandedKey = it },
                            onChange = vm::setCustomAccent,
                            onReset = null
                        )
                        ActionChip(
                            icon = Icons.Filled.AutoAwesome,
                            label = stringResource(R.string.theme_accent_from_artwork),
                            enabled = artworkPoster != null && !pickingArtwork,
                            loading = pickingArtwork
                        ) {
                            pickingArtwork = true
                            scope.launch {
                                val color = artworkPoster?.let { accentFromPoster(context, it) }
                                if (color != null) vm.setCustomAccent(color)
                                pickingArtwork = false
                            }
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

                    // ------------------------------------------------------ colors
                    StudioCard(
                        title = stringResource(R.string.studio_section_colors),
                        action = if (extras.hasColorOverrides()) {
                            {
                                ActionChip(
                                    icon = Icons.Filled.RestartAlt,
                                    label = stringResource(R.string.studio_reset_colors)
                                ) { vm.resetCustomColorOverrides() }
                            }
                        } else null
                    ) {
                        colorTokens.forEach { token ->
                            val override = token.read(extras)
                            ColorRow(
                                id = token.key,
                                label = stringResource(token.labelRes),
                                color = override?.let { Color(it.toInt()) }
                                    ?: token.current(live).copy(alpha = 1f),
                                overridden = override != null,
                                expandedKey = expandedKey,
                                onExpand = { expandedKey = it },
                                onChange = { c ->
                                    vm.updateCustomExtras { token.write(it, c.toLongArgb()) }
                                },
                                onReset = {
                                    vm.updateCustomExtras { token.write(it, null) }
                                }
                            )
                        }
                    }

                    // ---------------------------------------------------- surfaces
                    StudioCard(title = stringResource(R.string.theme_surface)) {
                        SwitchRow(
                            title = stringResource(R.string.theme_glass_surfaces),
                            subtitle = stringResource(R.string.theme_glass_surfaces_desc),
                            checked = vm.customGlass,
                            onCheckedChange = vm::setCustomGlass
                        )
                        SwitchRow(
                            title = stringResource(R.string.theme_high_contrast),
                            subtitle = stringResource(R.string.theme_high_contrast_desc),
                            checked = vm.customHighContrast,
                            onCheckedChange = vm::setCustomHighContrast
                        )
                    }

                    // -------------------------------------------------------- share
                    StudioCard(title = stringResource(R.string.studio_section_share)) {
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
                        codeMessage?.let {
                            Text(
                                stringResource(it),
                                fontSize = 10.sp,
                                color = p.muted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun StudioHeader(onClose: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.studio_title),
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = if (p.isNeoBrutalist) FontFamily.Monospace else FontFamily.Default,
            color = p.text
        )
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(p.chip)
                .clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.close),
                tint = p.text,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StudioCard(
    title: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(if (p.isNeoBrutalist) 8.dp else 20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(p.card)
            .border(if (p.isNeoBrutalist) 2.dp else 1.dp, p.border, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (p.isNeoBrutalist) title.uppercase() else title,
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = if (p.isNeoBrutalist) FontFamily.Monospace else FontFamily.Default,
                color = if (p.isNeoBrutalist) p.text else p.muted
            )
            action?.invoke()
        }
        content()
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(if (p.isNeoBrutalist) 6.dp else 10.dp)
    Row(
        Modifier
            .clip(shape)
            .background(if (enabled) p.chip else p.chip.copy(alpha = p.chip.alpha * 0.5f))
            .then(if (p.isNeoBrutalist) Modifier.border(2.dp, p.border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = p.accent)
        } else {
            Icon(icon, null, tint = if (enabled) p.accent else p.muted, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) p.text else p.muted
        )
    }
}

@Composable
private fun ImageBackgroundControls(
    extras: CustomThemeExtras,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    onChange: ((CustomThemeExtras) -> CustomThemeExtras) -> Unit
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(if (p.isNeoBrutalist) 8.dp else 14.dp)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        extras.imagePath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(shape)
                    .border(if (p.isNeoBrutalist) 2.dp else 1.dp, p.border, shape)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip(
                icon = Icons.Filled.Image,
                label = stringResource(
                    if (extras.imagePath == null) R.string.studio_pick_image else R.string.studio_change_image
                ),
                onClick = onPick
            )
            if (extras.imagePath != null) {
                ActionChip(
                    icon = Icons.Filled.Close,
                    label = stringResource(R.string.studio_remove_image),
                    onClick = onRemove
                )
            }
        }
        StudioSlider(
            label = stringResource(R.string.studio_image_dim, (extras.imageDim * 100).roundToInt()),
            value = extras.imageDim,
            range = 0f..0.85f
        ) { v -> onChange { it.copy(imageDim = v) } }
        StudioSlider(
            label = stringResource(R.string.studio_image_blur, extras.imageBlur.roundToInt()),
            value = extras.imageBlur,
            range = 0f..25f
        ) { v -> onChange { it.copy(imageBlur = v) } }
        StudioSlider(
            label = stringResource(R.string.studio_surface_opacity, (extras.surfaceOpacity * 100).roundToInt()),
            value = extras.surfaceOpacity,
            range = 0.25f..1f
        ) { v -> onChange { it.copy(surfaceOpacity = v) } }
        Text(
            stringResource(R.string.studio_image_note),
            fontSize = 10.sp,
            color = p.muted,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val p = LocalPalette.current
    Column {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = p.text)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = p.accent,
                activeTrackColor = p.accent,
                inactiveTrackColor = p.chip
            )
        )
    }
}

/**
 * A labelled colour token. Tapping it opens the inline [ColorPickerPanel]; only
 * one row is expanded at a time (tracked by [expandedKey]).
 */
@Composable
private fun ColorRow(
    id: String,
    label: String,
    color: Color,
    overridden: Boolean,
    expandedKey: String?,
    onExpand: (String?) -> Unit,
    onChange: (Color) -> Unit,
    onReset: (() -> Unit)?
) {
    val p = LocalPalette.current
    val open = expandedKey == id
    val swatchShape = RoundedCornerShape(if (p.isNeoBrutalist) 6.dp else 10.dp)
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button) { onExpand(if (open) null else id) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = p.text)
                Text(
                    if (onReset != null && !overridden) stringResource(R.string.studio_color_auto)
                    else "#" + color.toHexRgb(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = p.muted
                )
            }
            if (overridden && onReset != null) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { onReset() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = stringResource(R.string.studio_color_reset),
                        tint = p.muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        AnimatedVisibility(visible = open) {
            Column(Modifier.padding(top = 6.dp, bottom = 8.dp)) {
                ColorPickerPanel(color = color, onColorChange = onChange)
                HorizontalDivider(
                    Modifier.padding(top = 10.dp),
                    color = p.border,
                    thickness = if (p.isNeoBrutalist) 2.dp else 1.dp
                )
            }
        }
    }
}

/** Saturation/value square, hue bar and hex field working on one colour. */
@Composable
internal fun ColorPickerPanel(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val initial = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var sat by remember { mutableFloatStateOf(initial[1]) }
    var value by remember { mutableFloatStateOf(initial[2]) }
    var lastEmitted by remember { mutableStateOf<Int?>(null) }
    val currentOnChange by rememberUpdatedState(onColorChange)

    // Follow outside changes (hex field, preset, reset) without fighting a drag.
    LaunchedEffect(color) {
        val argb = color.toArgb()
        if (argb != lastEmitted) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(argb, hsv)
            if (hsv[1] > 0.001f && hsv[2] > 0.001f) hue = hsv[0]
            sat = hsv[1]
            value = hsv[2]
        }
    }

    fun emit() {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        lastEmitted = argb
        currentOnChange(Color(argb))
    }

    val p = LocalPalette.current
    val boxShape = RoundedCornerShape(if (p.isNeoBrutalist) 6.dp else 12.dp)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Saturation (x) and value (y) for the current hue.
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(boxShape)
                .border(if (p.isNeoBrutalist) 2.dp else 1.dp, p.border, boxShape)
                .drawBehind {
                    drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor(hue))))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    val center = Offset(sat * size.width, (1f - value) * size.height)
                    drawCircle(Color.Black, radius = 11.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                    drawCircle(Color.White, radius = 9.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun update(pos: Offset) {
                            sat = (pos.x / size.width).coerceIn(0f, 1f)
                            value = 1f - (pos.y / size.height).coerceIn(0f, 1f)
                            emit()
                        }
                        update(down.position)
                        drag(down.id) { change ->
                            update(change.position)
                            change.consume()
                        }
                    }
                }
        )

        // Hue.
        val hueColors = remember { (0..6).map { hueColor(it * 60f) } }
        Box(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(CircleShape)
                .border(if (p.isNeoBrutalist) 2.dp else 1.dp, p.border, CircleShape)
                .drawBehind {
                    drawRect(Brush.horizontalGradient(hueColors))
                    val x = (hue / 360f) * size.width
                    val center = Offset(x.coerceIn(13.dp.toPx(), size.width - 13.dp.toPx()), size.height / 2)
                    drawCircle(Color.Black, radius = 12.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                    drawCircle(Color.White, radius = 10.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun update(pos: Offset) {
                            hue = (pos.x / size.width).coerceIn(0f, 1f) * 360f
                            emit()
                        }
                        update(down.position)
                        drag(down.id) { change ->
                            update(change.position)
                            change.consume()
                        }
                    }
                }
        )

        HexColorField(
            label = stringResource(R.string.studio_hex),
            color = color,
            onColorChange = onColorChange
        )
    }
}
