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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
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
    WelcomePage(R.drawable.welcome_bg_1, R.string.welcome_1_title, R.string.welcome_1_desc),
    WelcomePage(R.drawable.welcome_bg_2, R.string.welcome_2_title, R.string.welcome_2_desc),
    WelcomePage(R.drawable.welcome_bg_3, R.string.welcome_3_title, R.string.welcome_3_desc),
    WelcomePage(R.drawable.welcome_bg_4, R.string.welcome_4_title, R.string.welcome_4_desc),
    WelcomePage(R.drawable.welcome_bg_5, R.string.welcome_5_title, R.string.welcome_5_desc),
    WelcomePage(R.drawable.welcome_bg_6, R.string.welcome_6_title, R.string.welcome_6_desc)
)

// The welcome artwork is a fixed light showcase (cream-based illustrations),
// so the copy sitting on it uses fixed ink colours instead of the theme
// palette: readable on the art in every theme, neo-brutalism included.
private val WelcomeCream = Color(0xFFFDF8EF)
private val WelcomeInk = Color(0xFF171A21)
private val WelcomeMuted = Color(0xFF4B5160)

/**
 * First-run welcome slides. [onFinish] is called when the person taps
 * "Get started" on the last page or skips; the caller persists that so the
 * slides are only shown once.
 *
 * Each slide is a full-bleed illustration behind the whole screen. The art
 * crossfades between pages, drifts slower than the swipe (parallax) and
 * breathes with a slow Ken Burns zoom; the copy fades and rises with the
 * page offset, so the whole flow feels alive without any video assets.
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

    // Slow Ken Burns breathing shared by every slide's artwork.
    val kenBurns = rememberInfiniteTransition(label = "welcomeKenBurns")
    val kbScale by kenBurns.animateFloat(
        initialValue = 1.18f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcomeKbScale"
    )
    val kbDrift by kenBurns.animateFloat(
        initialValue = -0.012f,
        targetValue = 0.012f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcomeKbDrift"
    )

    // Back goes to the previous slide; on the first slide it leaves the app as usual.
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        WelcomeBackground(position = position, kbScale = kbScale, kbDrift = kbDrift)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Skip (kept in layout on the last page so nothing jumps). It sits
            // on the artwork, so it gets its own cream pill for readability.
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
                            .background(WelcomeCream.copy(alpha = 0.88f))
                            .clickable(role = Role.Button, onClick = onFinish)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (p.isNeoBrutalist) FontFamily.Monospace else FontFamily.Default,
                        color = WelcomeInk
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
                    offset = position - index
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

/**
 * The stacked full-bleed artworks. Only slides within one page of the
 * current position are composed; each one fades with its distance from
 * the pager position, shifts at a fraction of the swipe (parallax) and
 * shares the Ken Burns zoom/drift. A cream gradient scrim keeps the
 * lower third calm so the copy, dots and button stay readable.
 */
@Composable
private fun WelcomeBackground(position: Float, kbScale: Float, kbDrift: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        welcomePages.forEachIndexed { index, page ->
            val offset = position - index
            if (abs(offset) < 1.05f) {
                Image(
                    painter = painterResource(page.artRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = (1f - abs(offset)).coerceIn(0f, 1f)
                            translationX = offset * size.width * 0.15f
                            translationY = kbDrift * size.height
                            scaleX = kbScale
                            scaleY = kbScale
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.50f to Color.Transparent,
                        0.76f to WelcomeCream.copy(alpha = 0.88f),
                        1.0f to WelcomeCream
                    )
                )
        )
    }
}

@Composable
private fun WelcomePageContent(page: WelcomePage, offset: Float) {
    val p = LocalPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .graphicsLayer {
                alpha = (1f - abs(offset)).coerceIn(0f, 1f)
                translationY = offset * 90f
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            stringResource(page.titleRes),
            fontSize = 27.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = if (p.isNeoBrutalist) FontFamily.Monospace else FontFamily.Default,
            color = WelcomeInk,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(page.descRes),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = WelcomeMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
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
                    .background(if (selected) p.accent else WelcomeInk.copy(alpha = 0.22f))
            )
        }
    }
}
