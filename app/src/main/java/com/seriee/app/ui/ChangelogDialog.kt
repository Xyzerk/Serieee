package com.seriee.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.seriee.app.R

@Composable
fun ChangelogDialog(
    versionName: String,
    onDismiss: () -> Unit
) {
    val p = LocalPalette.current

    // Only the current release is shown. When preparing a new version, add its
    // changelog_* strings and replace this list; older entries stay in CHANGELOG.md.
    val items = listOf(
        stringResource(R.string.changelog_29),
        stringResource(R.string.changelog_30),
        stringResource(R.string.changelog_31),
        stringResource(R.string.changelog_32),
        stringResource(R.string.changelog_33)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = if (p.isGlass) p.surface.copy(alpha = if (p.isLight) 0.90f else 0.84f) else p.surface,
            border = BorderStroke(1.dp, p.border)
        ) {
            LiquidSheetHost {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.changelog_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.text
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(p.primary)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                stringResource(R.string.changelog_version, versionName),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.onAccent
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(p.chip)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.changelog_close),
                            tint = p.muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                items.forEach { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(16.dp),
                                tint = p.card,
                                blurRadius = 12.dp,
                                refractionHeight = 8.dp,
                                refractionAmount = 14.dp
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(50))
                                .background(p.star.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Star, null, tint = p.star, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text,
                            fontSize = 12.sp,
                            color = p.text,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                PrimaryButton(
                    text = stringResource(R.string.changelog_got_it),
                    onClick = onDismiss
                )
            }
            }
        }
    }
}
