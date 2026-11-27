package com.seriee.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seriee.app.R
import kotlin.math.abs
import kotlinx.coroutines.launch

private data class WelcomePage(
    @DrawableRes val artRes: Int,
    val titleRes: Int,
    val descRes: Int
)

private val welcomePages = listOf(
    WelcomePage(R.drawable.welcome_illus_1, R.string.welcome_1_title, R.string.welcome_1_desc),
    WelcomePage(R.drawable.welcome_illus_2, R.string.welcome_2_title, R.string.welcome_2_desc),
    WelcomePage(R.drawable.welcome_illus_3, R.string.welcome_3_title, R.string.welcome_3_desc),
    WelcomePage(R.drawable.welcome_illus_4, R.string.welcome_4_title, R.string.welcome_4_desc),
    WelcomePage(R.drawable.welcome_illus_5, R.string.welcome_5_title, R.string.welcome_5_desc),
    WelcomePage(R.drawable.welcome_illus_6, R.string.welcome_6_title, R.string.welcome_6_desc)
)

/**
 * First-run welcome slides. [onFinish] is called when the person taps
 * "Get started" on the last page or skips; the caller persists that so the
 * slides are only shown once.
 *
 * Each slide carries a Material 3 spot illustration. The motion stays on
 * even though the art is static: the card parallaxes against the swipe,
 * floats with a gentle bob and breathes with a slow inner zoom, while
 * the copy fades and rises with the page offset.
 */
@Composable
fun WelcomeScreen(onFinish: () -> Unit) {
    val p = LocalPalette.current
    val pagerState = rememberPagerState { welcomePages.size }
    val scope = rememberCoroutineScope()
    val lastIndex = welcomePages.lastIndex
    val isLast = pagerState.currentPage == lastIndex
    // Fractional pager position: currentPage is where the pager rests,
    // the offset fraction carries the in-flight swipe.
    val position = pagerState.currentPage + pagerState.currentPageOffsetFraction

    // Shared gentle float + breathing zoom for every slide's illustration.
    val floatAnim = rememberInfiniteTransition(label = "welcomeFloat")
    val bob by floatAnim.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcomeBob"
    )
    val breathe by floatAnim.animateFloat(
        initialValue = 1.03f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcomeBreathe"
    )

    // Back goes to the previous slide; on the first slide it leaves the app as usual.
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
            .then(
                if (p.gradient != null && p.gradient.size >= 2 && !p.isNeoBrutalist) {
                    Modifier.background(Brush.verticalGradient(p.gradient))
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Skip (kept in layout on the last page so nothing jumps).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLast) {
                    Text(
                        stringResource(R.string.welcome_skip),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(role = Role.Button, onClick = onFinish)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (p.isNeoBrutalist) FontFamily.Monospace else FontFamily.Default,
                        color = p.muted
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                WelcomePageContent(
                    page = welcomePages[index],
                    offset = position - index,
                    bob = bob,
                    breathe = breathe
                )
            }

            PageDots(
                count = welcomePages.size,
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 20.dp)
            )

            PrimaryButton(
                text = stringResource(
                    if (isLast) R.string.welcome_start else R.string.welcome_next
                ),
                onClick = {
                    if (isLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                gradient = true,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun WelcomePageContent(page: WelcomePage, offset: Float, bob: Float, breathe: Float) {
    val p = LocalPalette.current
    val clamped = abs(offset).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val shape = RoundedCornerShape(if (p.isNeoBrutalist) 16.dp else 36.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .graphicsLayer {
                    alpha = 1f - clamped
                    translationX = -offset * 56f
                    translationY = bob
                    val scale = 1f - 0.05f * clamped
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .then(
                    if (p.isNeoBrutalist) Modifier.border(3.dp, p.border, shape) else Modifier
                )
        ) {
            Image(
                painter = painterResource(page.artRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = breathe
                        scaleY = breathe
                    },
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(40.dp))
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = 1f - clamped
                translationY = offset * 90f
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(page.titleRes),
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = if (p.isNeoBrutalist) FontFamily.Monospace else FontFamily.Default,
                color = p.text,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(page.descRes),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = p.muted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val description = stringResource(R.string.welcome_page_of, current + 1, count)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            val selected = i == current
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dotWidth")
            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (selected) p.accent else p.muted.copy(alpha = 0.35f))
            )
        }
    }
}
