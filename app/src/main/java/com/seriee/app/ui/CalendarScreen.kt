package com.seriee.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seriee.app.data.MediaItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * F4: Weekly airing calendar. Groups every title with a known next-airing
 * time (plus Coming Soon releases) by local calendar day, soonest first.
 */
@Composable
fun CalendarScreen(
    list: List<MediaItem>,
    padding: PaddingValues,
    onOpen: (MediaItem) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val grouped = remember(list) {
        val entries = list.mapNotNull { item ->
            val at = item.nextAirAt ?: item.releaseDate ?: return@mapNotNull null
            val date = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
            Triple(date, at, item)
        }.sortedWith(compareBy({ it.first }, { it.second }))
        entries.groupBy { it.first }
    }
    val today = remember { LocalDate.now(zone) }
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.getDefault()) }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                "Kalender Tayang",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (grouped.isEmpty()) {
            item {
                Text(
                    "Belum ada jadwal tayang. Judul yang sedang tayang (airing) atau Coming Soon akan muncul di sini.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        grouped.forEach { (date, dayEntries) ->
            item(key = "day-$date") {
                val label = when (date) {
                    today -> "Hari ini • ${date.format(dayFmt)}"
                    today.plusDays(1) -> "Besok • ${date.format(dayFmt)}"
                    else -> date.format(dayFmt)
                }
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                )
            }
            items(dayEntries, key = { "cal-${it.third.id}-${it.second}" }) { (_, at, item) ->
                Row(
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = SerieeMotion.enterTween(),
                            placementSpec = SerieeMotion.SmoothSpring,
                            fadeOutSpec = SerieeMotion.exitTween()
                        )
                        .fillMaxWidth()
                        .clickable { onOpen(item) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Poster(item.poster, Modifier.size(width = 42.dp, height = 62.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        val epLabel = if (item.nextEpisode != null) {
                            "Episode ${item.nextEpisode}" +
                                (item.nextEpisodeSeason?.let { " • Season $it" } ?: "")
                        } else {
                            "Rilis"
                        }
                        Text(
                            "$epLabel • ${Instant.ofEpochMilli(at).atZone(zone).format(timeFmt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}
