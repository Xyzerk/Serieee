package com.seriee.app.ui

import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The user's custom background picture with its readability scrim. Draws
 * nothing when no image is configured or the file can no longer be read, so the
 * solid/gradient background underneath simply shows through.
 */
@Composable
fun ThemeBackdropImage(modifier: Modifier = Modifier) {
    val backdrop = LocalThemeBackdrop.current ?: return
    val p = LocalPalette.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, backdrop.path) {
        value = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(backdrop.path)?.asImageBitmap() }.getOrNull()
        }
    }
    val image = bitmap ?: return
    val blur = if (backdrop.blurDp > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(backdrop.blurDp.dp, BlurredEdgeTreatment.Unbounded)
    } else Modifier

    Box(modifier.fillMaxSize().clipToBounds()) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().then(blur)
        )
        // Dark themes get a black scrim, light themes a white one, so the
        // text colours chosen for the theme stay legible on any photo.
        Box(
            Modifier
                .fillMaxSize()
                .background((if (p.isLight) Color.White else Color.Black).copy(alpha = backdrop.dim))
        )
    }
}
