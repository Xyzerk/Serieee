package com.seriee.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.seriee.app.data.WatchStatus

/** Backdrop capturing the ambient wallpaper/gradient behind in-page glass cards. */
val LocalLiquidBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** Backdrop capturing both background and scrolling content behind floating chrome. */
val LocalChromeBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

object SerieeType {
    val display = androidx.compose.ui.text.TextStyle(
        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp
    )
    val title = androidx.compose.ui.text.TextStyle(
        fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp
    )
    val section = androidx.compose.ui.text.TextStyle(
        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp
    )
    val body = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp
    )
    val label = androidx.compose.ui.text.TextStyle(
        fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp
    )
    val caption = androidx.compose.ui.text.TextStyle(
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp
    )
}

object SerieeShape {
    val pill = RoundedCornerShape(50)
    val chip = RoundedCornerShape(14.dp)
    val field = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(22.dp)
    val sheet = RoundedCornerShape(28.dp)
}

object SerieeSpace {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
}

object SerieeMotion {
    const val quick = 180
    const val standard = 240
    const val emphasized = 320
}

/** Status colours shared by cards, badges and the detail sheet. */
@Composable
fun statusColor(status: WatchStatus): Color {
    val p = LocalPalette.current
    return when (status) {
        WatchStatus.Watching -> p.primary
        WatchStatus.Completed -> p.success
        WatchStatus.PlanToWatch -> p.muted
        WatchStatus.ComingSoon -> p.warning
        WatchStatus.OnHold -> p.reminder
        WatchStatus.Dropped -> p.danger
    }
}

/** Specular highlight used on glass borders so they catch the light. */
private fun SeriePalette.specularColors(): List<Color> {
    val strong = if (isLight) 0.92f else 0.42f
    val soft = if (isLight) 0.50f else 0.14f
    return listOf(
        Color.White.copy(alpha = strong),
        Color.White.copy(alpha = 0.06f),
        Color.White.copy(alpha = soft)
    )
}

/**
 * Theme-aware outline: a brushed specular edge on glass styles, the palette
 * border everywhere else.
 */
fun Modifier.themedBorder(p: SeriePalette, shape: Shape, width: Dp = 1.dp): Modifier =
    border(
        width = width,
        brush = if (p.isGlass) Brush.linearGradient(p.specularColors()) else SolidColor(p.border),
        shape = shape
    )

/**
 * Apple Liquid Glass surface powered by `io.github.kyant0:backdrop`.
 *
 * Samples [backdrop] through vibrancy + Gaussian blur + optical SDF lens
 * refraction and RGB chromatic dispersion when [palette.isGlass] is active,
 * falling back cleanly to solid themed surfaces when [reduceTransparency] or
 * an opaque style is chosen.
 */
fun Modifier.liquidGlass(
    shape: Shape,
    backdrop: LayerBackdrop? = null,
    tint: Color? = null,
    blurRadius: Dp = 18.dp,
    refractionHeight: Dp = 12.dp,
    refractionAmount: Dp = 24.dp,
    chromaticAberration: Boolean = true,
    elevated: Boolean = false,
    withHighlight: Boolean = true,
    withBorder: Boolean = true
): Modifier = composed {
    val p = LocalPalette.current
    val activeBackdrop = backdrop ?: LocalLiquidBackdrop.current
    val resolvedTint = tint ?: if (p.isGlass) {
        if (p.isLight) Color.White.copy(alpha = 0.46f)
        else Color(0xFF0E1729).copy(alpha = 0.48f)
    } else {
        p.card
    }

    if (p.isGlass && activeBackdrop != null) {
        this
            .drawBackdrop(
                backdrop = activeBackdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    if (blurRadius > 0.dp) {
                        blur(blurRadius.toPx())
                    }
                    if (p.isLiquidGlass && refractionAmount > 0.dp) {
                        lens(
                            refractionHeight = refractionHeight.toPx(),
                            refractionAmount = refractionAmount.toPx(),
                            chromaticAberration = chromaticAberration
                        )
                    }
                },
                highlight = if (withHighlight) {
                    { Highlight.Default }
                } else null,
                shadow = if (elevated) {
                    { Shadow.Default }
                } else null,
                onDrawSurface = {
                    drawRect(resolvedTint)
                }
            )
            .then(
                if (withBorder) Modifier.themedBorder(p, shape, width = 0.75.dp) else Modifier
            )
    } else {
        // No live backdrop (API < 31) or an opaque / reduced-transparency style:
        // composite the tint over the page background so the fallback keeps a
        // readable contrast instead of washing out over the gradient, and
        // restore the elevation that the blur effect would have provided.
        val fallbackTint = tint ?: p.card
        val surface = if (p.isGlass) fallbackTint.compositeOver(p.bg) else fallbackTint
        this
            .then(
                if (elevated && p.isGlass) Modifier.shadow(6.dp, shape, clip = false) else Modifier
            )
            .clip(shape)
            .background(surface)
            .then(
                if (withBorder) {
                    // The white specular edge is invisible on pale backgrounds;
                    // use the palette border when the glass is unavailable.
                    if (p.isGlass) Modifier.border(1.dp, p.border, shape)
                    else Modifier.themedBorder(p, shape)
                } else {
                    Modifier
                }
            )
    }
}

/** Translucent floating chrome (nav bar, top bar) with strong Liquid Glass optics. */
fun Modifier.glassChrome(
    p: SeriePalette,
    shape: Shape,
    backdrop: LayerBackdrop? = null
): Modifier = composed {
    val chromeBackdrop = backdrop ?: LocalChromeBackdrop.current ?: LocalLiquidBackdrop.current
    val surfaceTint = if (p.isGlass) {
        if (p.isLight) Color.White.copy(alpha = 0.42f)
        else Color(0xFF0B1324).copy(alpha = 0.48f)
    } else {
        p.surface
    }
    liquidGlass(
        shape = shape,
        backdrop = chromeBackdrop,
        tint = surfaceTint,
        blurRadius = 12.dp,
        refractionHeight = 18.dp,
        refractionAmount = 36.dp,
        chromaticAberration = true,
        elevated = true,
        withHighlight = true,
        withBorder = true
    )
}

/**
 * Provides an ambient glass [LayerBackdrop] inside standalone Android windows
 * such as [ModalBottomSheet] and [Dialog], so inner cards, fields, and buttons
 * still exhibit Liquid Glass refraction and dispersion.
 */
@Composable
fun LiquidSheetHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val p = LocalPalette.current
    val sheetBackdrop = rememberLayerBackdrop()
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(sheetBackdrop)
                .drawBehind {
                    drawRect(if (p.isGlass) p.surface.copy(alpha = if (p.isLight) 0.90f else 0.84f) else p.surface)
                    if (p.isGlass) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    p.primary.copy(alpha = if (p.isLight) 0.16f else 0.22f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.15f, size.height * 0.12f),
                                radius = size.maxDimension * 0.65f
                            ),
                            center = Offset(size.width * 0.15f, size.height * 0.12f),
                            radius = size.maxDimension * 0.65f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    p.accent.copy(alpha = if (p.isLight) 0.14f else 0.18f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.88f, size.height * 0.72f),
                                radius = size.maxDimension * 0.6f
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.72f),
                            radius = size.maxDimension * 0.6f
                        )
                    }
                }
        )
        CompositionLocalProvider(LocalLiquidBackdrop provides sheetBackdrop) {
            content()
        }
    }
}

/**
 * Full-screen backdrop. Liquid styles drift their aurora gradient and luminous
 * color orbs slowly so refracting glass cards catch shifting highlights.
 */
@Composable
fun SerieeBackground(p: SeriePalette, modifier: Modifier = Modifier) {
    val gradient = p.gradient
    Box(modifier = modifier.fillMaxSize().background(p.bg)) {
        if (gradient == null) return@Box
        if (p.animatedGradient) {
            val transition = rememberInfiniteTransition(label = "liquidBackground")
            val drift by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 16_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "liquidDrift"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = gradient,
                                start = Offset(size.width * 0.25f * drift, 0f),
                                end = Offset(size.width * (1f - 0.25f * drift), size.height)
                            )
                        )
                        if (p.isGlass) {
                            val orbA = Offset(
                                x = size.width * (0.18f + 0.22f * drift),
                                y = size.height * (0.14f + 0.08f * drift)
                            )
                            val orbB = Offset(
                                x = size.width * (0.85f - 0.25f * drift),
                                y = size.height * (0.52f - 0.10f * drift)
                            )
                            val orbC = Offset(
                                x = size.width * (0.30f + 0.35f * drift),
                                y = size.height * 0.86f
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        p.primary.copy(alpha = if (p.isLight) 0.30f else 0.34f),
                                        Color.Transparent
                                    ),
                                    center = orbA,
                                    radius = size.width * 0.65f
                                ),
                                center = orbA,
                                radius = size.width * 0.65f
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        p.accent.copy(alpha = if (p.isLight) 0.24f else 0.26f),
                                        Color.Transparent
                                    ),
                                    center = orbB,
                                    radius = size.width * 0.60f
                                ),
                                center = orbB,
                                radius = size.width * 0.60f
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        (gradient.lastOrNull() ?: p.accent).copy(
                                            alpha = if (p.isLight) 0.28f else 0.32f
                                        ),
                                        Color.Transparent
                                    ),
                                    center = orbC,
                                    radius = size.width * 0.70f
                                ),
                                center = orbC,
                                radius = size.width * 0.70f
                            )
                        }
                    }
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(gradient)))
        }
    }
}

@Composable
fun SerieeSurface(
    modifier: Modifier = Modifier,
    shape: Shape = SerieeShape.card,
    level: Int = 1,
    content: @Composable () -> Unit
) {
    val p = LocalPalette.current
    val bg = when (level) {
        0 -> p.bg
        2 -> p.elevated
        else -> p.card
    }
    Box(
        modifier = if (level > 0 && p.isGlass) {
            modifier.liquidGlass(
                shape = shape,
                tint = bg,
                blurRadius = 16.dp,
                refractionHeight = 10.dp,
                refractionAmount = 18.dp
            )
        } else {
            modifier
                .clip(shape)
                .background(bg)
                .then(if (level > 0) Modifier.themedBorder(p, shape) else Modifier)
        }
    ) {
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.text)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, color = p.muted)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun IconPillButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color? = null,
    backdrop: LayerBackdrop? = null,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    val effectiveBackdrop = backdrop ?: LocalLiquidBackdrop.current
    Box(
        modifier = modifier
            .size(40.dp)
            .liquidGlass(
                shape = CircleShape,
                backdrop = effectiveBackdrop,
                tint = if (p.isGlass) {
                    if (p.isLight) Color.White.copy(alpha = 0.52f)
                    else Color.White.copy(alpha = 0.14f)
                } else p.chip,
                blurRadius = 14.dp,
                refractionHeight = 8.dp,
                refractionAmount = 16.dp
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint ?: p.text.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    showChevron: Boolean = true,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint ?: p.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = p.text,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                value,
                fontSize = 12.sp,
                color = p.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
        }
        if (showChevron) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = p.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun RadioCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    val tint = if (selected) {
        p.primary.copy(alpha = if (p.isGlass) 0.22f else 0.12f)
    } else if (p.isGlass) {
        if (p.isLight) Color.White.copy(alpha = 0.44f) else Color.White.copy(alpha = 0.08f)
    } else {
        p.chip
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = SerieeShape.field,
                tint = tint,
                blurRadius = 14.dp,
                refractionHeight = 8.dp,
                refractionAmount = 14.dp,
                withBorder = !selected
            )
            .then(
                if (selected) Modifier.border(1.dp, p.primary.copy(alpha = 0.65f), SerieeShape.field)
                else Modifier
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) p.primary else p.muted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.text)
            Text(subtitle, fontSize = 11.sp, color = p.muted, lineHeight = 15.sp)
        }
    }
}

@Composable
fun SegmentedTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = SerieeShape.pill,
                tint = if (p.isGlass) {
                    if (p.isLight) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f)
                } else p.chip,
                blurRadius = 14.dp,
                refractionHeight = 8.dp,
                refractionAmount = 14.dp
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(SerieeShape.pill)
                    .background(
                        if (active) {
                            Brush.horizontalGradient(listOf(p.primary, p.accent))
                        } else {
                            SolidColor(Color.Transparent)
                        }
                    )
                    .then(
                        if (active && p.isGlass) {
                            Modifier.border(0.75.dp, Color.White.copy(alpha = 0.45f), SerieeShape.pill)
                        } else Modifier
                    )
                    .selectable(selected = active, role = Role.Tab) { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) p.onAccent else p.muted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Boolean = false
) {
    val p = LocalPalette.current
    val tintColor = if (enabled) {
        p.primary.copy(alpha = if (p.isGlass) 0.78f else 1f)
    } else {
        p.chip
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .liquidGlass(
                shape = SerieeShape.pill,
                tint = tintColor,
                blurRadius = 14.dp,
                refractionHeight = 10.dp,
                refractionAmount = 18.dp,
                elevated = enabled && p.isGlass,
                withBorder = true
            )
            .then(
                if (enabled && gradient) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(
                                p.primary.copy(alpha = if (p.isGlass) 0.68f else 1f),
                                p.accent.copy(alpha = if (p.isGlass) 0.68f else 1f)
                            )
                        )
                    )
                } else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) p.onAccent else p.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .liquidGlass(
                shape = SerieeShape.pill,
                tint = if (p.isGlass) {
                    if (p.isLight) Color.White.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.12f)
                } else p.chip,
                blurRadius = 14.dp,
                refractionHeight = 10.dp,
                refractionAmount = 18.dp
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.text)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    hint: String,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .liquidGlass(
                    shape = CircleShape,
                    tint = if (p.isGlass) {
                        if (p.isLight) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.12f)
                    } else p.chip,
                    blurRadius = 16.dp,
                    refractionHeight = 14.dp,
                    refractionAmount = 24.dp,
                    elevated = true
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(28.dp))
        }
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.text, textAlign = TextAlign.Center)
        Text(hint, fontSize = 12.sp, color = p.muted, textAlign = TextAlign.Center, lineHeight = 17.sp)
        if (ctaLabel != null && onCta != null) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .liquidGlass(
                        shape = SerieeShape.pill,
                        tint = p.primary.copy(alpha = if (p.isGlass) 0.80f else 1f),
                        blurRadius = 14.dp,
                        refractionHeight = 10.dp,
                        refractionAmount = 18.dp,
                        elevated = p.isGlass
                    )
                    .clickable(onClick = onCta)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(ctaLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.onAccent)
            }
        }
    }
}

/** Shared drag handle for every [androidx.compose.material3.ModalBottomSheet]. */
@Composable
fun SheetDragHandle() {
    val p = LocalPalette.current
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .size(width = 40.dp, height = 5.dp)
            .clip(RoundedCornerShape(50))
            .background(if (p.isGlass) Color.White.copy(alpha = if (p.isLight) 0.7f else 0.35f) else p.border)
    )
}

@Composable
fun GradientProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    colors: List<Color>? = null,
    track: Color? = null
) {
    val p = LocalPalette.current
    val resolvedTrack = track ?: if (p.isGlass) {
        if (p.isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.14f)
    } else p.chip
    val brush = Brush.horizontalGradient(colors ?: listOf(p.primary, p.accent))
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(resolvedTrack)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(brush)
        )
    }
}

@Composable
fun StatusBadge(status: WatchStatus, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val color = statusColor(status)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = if (p.isGlass) 0.22f else 0.16f))
            .then(
                if (p.isGlass) Modifier.border(0.75.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
                else Modifier
            )
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (status == WatchStatus.Completed) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(3.dp))
            }
            Text(
                stringResource(status.labelRes),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
