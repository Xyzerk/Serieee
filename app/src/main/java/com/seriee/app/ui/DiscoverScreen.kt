package com.seriee.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seriee.app.data.MediaItem
import com.seriee.app.data.MetadataRepository
import com.seriee.app.data.MetadataResult
import kotlinx.coroutines.launch

/**
 * F5: Discover new anime from AniList — Trending now or this season's
 * popular titles — and add them straight to the library.
 */
@Composable
fun DiscoverScreen(
    list: List<MediaItem>,
    padding: PaddingValues,
    onAddFromMetadata: (MetadataResult) -> Unit
) {
    val repo = remember { MetadataRepository() }
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(0) } // 0 = Trending, 1 = Musim ini
    var results by remember { mutableStateOf<List<MetadataResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    fun load() {
        loading = true
        failed = false
        scope.launch {
            val res = if (mode == 0) repo.discoverTrending() else repo.discoverSeasonal()
            res.onSuccess { results = it }.onFailure { failed = true }
            loading = false
        }
    }

    LaunchedEffect(mode) { load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                "Discover",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Trending") })
                FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Musim Ini") })
            }
        }
        if (loading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }
        } else if (failed) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Gagal memuat dari AniList. Periksa koneksi internet.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { load() }) { Text("Coba Lagi") }
                }
            }
        }
        items(results, key = { "disc-${it.source}-${it.sourceId}" }) { r ->
            val alreadyAdded = list.any { it.source == r.source && it.sourceId == r.sourceId && r.sourceId.isNotBlank() }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Poster(r.posterUrl, Modifier.size(width = 42.dp, height = 62.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    val meta = listOfNotNull(
                        r.year.takeIf { it.isNotBlank() },
                        r.tags.take(3).joinToString(", ").takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onAddFromMetadata(r) },
                    enabled = !alreadyAdded
                ) { Text(if (alreadyAdded) "Ada" else "+ Tambah") }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}
