package com.seriee.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay

/**
 * A clock that ticks while composed and re-reads immediately on resume, so
 * memoized countdown strings cannot stay frozen on a card left on screen.
 */
@Composable
fun rememberNow(intervalMs: Long = 60_000L): Long {
    val context = LocalContext.current
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    return produceState(System.currentTimeMillis(), intervalMs, resumeTick) {
        value = System.currentTimeMillis()
        while (true) {
            delay(intervalMs)
            value = System.currentTimeMillis()
        }
    }.value
}
