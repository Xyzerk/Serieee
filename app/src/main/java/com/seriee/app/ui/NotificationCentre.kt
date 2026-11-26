package com.seriee.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seriee.app.R
import com.seriee.app.data.AiringRules
import com.seriee.app.data.MediaItem
import com.seriee.app.data.WatchStatus
import com.seriee.app.reminder.ReminderScheduler

/** One row of the notification centre. */
data class NotificationEntry(
    val item: MediaItem,
    val detail: String
)

/**
 * Vetro P2a: the two notification-centre sections, computed purely from the
 * library. Muted titles are always excluded.
 *
 * Returns (airing with a known next episode, quiet reminders) where a quiet
 * reminder is an unfinished title nobody has touched for over three weeks.
 */
fun notificationCentreEntries(
    items: List<MediaItem>,
    now: Long = System.currentTimeMillis()
): Pair<List<NotificationEntry>, List<NotificationEntry>> {
    val airing = items
        .filter { !it.notificationsMuted && AiringRules.isOngoing(it) && it.nextAirAt != null }
        .sortedBy { it.nextAirAt }
        .map { item ->
            NotificationEntry(
                item = item,
                detail = listOfNotNull(
                    item.nextEpisode?.let { "E$it" },
                    item.nextAirAt?.let { ts ->
                        val days = ReminderScheduler.daysUntilRelease(ts, now)
                        if (days > 0) "in ${days}d" else "today"
                    }
                ).joinToString(" • ")
            )
        }
    val quietCutoff = now - 21L * 86_400_000L
    val quiet = items
        .filter {
            !it.notificationsMuted &&
                (it.status == WatchStatus.Watching || it.status == WatchStatus.OnHold) &&
                it.watchedEpisodes > 0 && it.watchedEpisodes < it.totalEpisodes &&
                it.updatedAt in 1 until quietCutoff
        }
        .sortedBy { it.updatedAt }
        .map { item ->
            val days = ((now - item.updatedAt) / 86_400_000L).toInt()
            NotificationEntry(
                item = item,
                detail = "Paused ${days}d • ${item.watchedEpisodes}/${item.totalEpisodes} episodes"
            )
        }
    return airing to quiet
}

@Composable
fun NotificationCentreDialog(
    items: List<MediaItem>,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit
) {
    val p = LocalPalette.current
    val (airing, quiet) = remember(items) { notificationCentreEntries(items) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surface,
        title = {
            Text(
                stringResource(R.string.notification_centre_title),
                fontWeight = FontWeight.Bold,
                color = p.text
            )
        },
        text = {
            if (airing.isEmpty() && quiet.isEmpty()) {
                Text(
                    stringResource(R.string.notification_centre_empty),
                    fontSize = 13.sp,
                    color = p.muted
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (airing.isNotEmpty()) {
                        item(key = "nc-airing-head") {
                            Text(
                                stringResource(R.string.notification_centre_airing),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.text,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(airing, key = { "airing-${it.item.id}" }) { entry ->
                            NotificationRow(entry) { onOpen(entry.item.id) }
                        }
                    }
                    if (quiet.isNotEmpty()) {
                        item(key = "nc-quiet-head") {
                            Text(
                                stringResource(R.string.notification_centre_quiet),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.text,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(quiet, key = { "quiet-${it.item.id}" }) { entry ->
                            NotificationRow(entry) { onOpen(entry.item.id) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun NotificationRow(entry: NotificationEntry, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                entry.item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = p.text,
                maxLines = 1
            )
            if (entry.detail.isNotBlank()) {
                Text(entry.detail, fontSize = 12.sp, color = p.muted)
            }
        }
    }
}
