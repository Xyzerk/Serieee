package com.seriee.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seriee.app.R
import com.seriee.app.SerieeViewModel

/**
 * Standalone sheet for the optional metadata API keys (TMDB and OMDb).
 *
 * These used to live at the bottom of [BackupRestoreDialog]; they now have
 * their own entry in the overflow menu so they are easier to find.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysDialog(
    vm: SerieeViewModel,
    onDismiss: () -> Unit
) {
    val p = LocalPalette.current
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(
            topStart = if (p.isNeoBrutalist) 8.dp else 28.dp,
            topEnd = if (p.isNeoBrutalist) 8.dp else 28.dp
        ),
        containerColor = if (p.isGlass) p.surface.copy(alpha = if (p.isLight) 0.90f else 0.84f) else p.surface,
        tonalElevation = if (p.isNeoBrutalist) 0.dp else BottomSheetDefaults.Elevation,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = { SheetDragHandle() }
    ) {
        LiquidSheetHost {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (p.isNeoBrutalist) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeoBrutalistColors.Yellow)
                                .border(2.dp, p.border, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Key,
                                contentDescription = null,
                                tint = NeoBrutalistColors.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Icon(
                            Icons.Filled.Key,
                            contentDescription = null,
                            tint = p.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.api_keys),
                        fontSize = 20.sp,
                        fontWeight = if (p.isNeoBrutalist) FontWeight.ExtraBold else FontWeight.Bold,
                        color = p.text,
                        modifier = Modifier.weight(1f)
                    )
                    if (p.isNeoBrutalist) {
                        NeoBrutalistBox(
                            onClick = onDismiss,
                            role = Role.Button,
                            backgroundColor = NeoBrutalistColors.OffWhite,
                            borderColor = p.border,
                            shadowColor = NeoBrutalistColors.Black,
                            borderWidth = 2.dp,
                            shadowOffset = 2.dp,
                            cornerRadius = 6.dp,
                            modifier = Modifier.size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = NeoBrutalistColors.Black)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = p.muted)
                        }
                    }
                }

                Text(
                    stringResource(R.string.api_keys_desc),
                    fontSize = 12.sp,
                    color = p.muted,
                    lineHeight = 17.sp
                )

                ApiKeySection(
                    title = stringResource(R.string.tmdb_api_key_title),
                    hint = stringResource(R.string.tmdb_api_key_hint),
                    savedKey = vm.tmdbKey,
                    saveLabel = stringResource(R.string.tmdb_api_key_save),
                    placeholder = stringResource(R.string.api_key_placeholder),
                    onSave = {
                        vm.setTmdbKey(it)
                        Toast.makeText(context, context.getString(R.string.tmdb_api_key_save), Toast.LENGTH_SHORT).show()
                    },
                    onClear = {
                        vm.setTmdbKey("")
                        Toast.makeText(context, context.getString(R.string.tmdb_api_key_cleared), Toast.LENGTH_SHORT).show()
                    }
                )

                ApiKeySection(
                    title = stringResource(R.string.anilist_token_title),
                    hint = stringResource(R.string.anilist_token_hint),
                    savedKey = vm.anilistToken,
                    saveLabel = stringResource(R.string.anilist_token_save),
                    placeholder = stringResource(R.string.api_key_placeholder),
                    onSave = {
                        vm.setAniListToken(it)
                        Toast.makeText(context, context.getString(R.string.anilist_token_save), Toast.LENGTH_SHORT).show()
                    },
                    onClear = {
                        vm.setAniListToken("")
                        Toast.makeText(context, context.getString(R.string.anilist_token_cleared), Toast.LENGTH_SHORT).show()
                    }
                )

                ApiKeySection(
                    title = stringResource(R.string.omdb_api_key_title),
                    hint = stringResource(R.string.omdb_api_key_hint),
                    savedKey = vm.omdbKey,
                    saveLabel = stringResource(R.string.omdb_api_key_save),
                    placeholder = stringResource(R.string.api_key_placeholder),
                    onSave = {
                        vm.setOmdbKey(it)
                        Toast.makeText(context, context.getString(R.string.omdb_api_key_save), Toast.LENGTH_SHORT).show()
                    },
                    onClear = {
                        vm.setOmdbKey("")
                        Toast.makeText(context, context.getString(R.string.omdb_api_key_cleared), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun ApiKeySection(
    title: String,
    hint: String,
    savedKey: String,
    saveLabel: String,
    placeholder: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    val p = LocalPalette.current
    // Keyed on savedKey so saving/clearing resets the field to the stored value.
    var input by remember(savedKey) { mutableStateOf(savedKey) }
    var revealed by remember(savedKey) { mutableStateOf(savedKey.isBlank()) }
    val saved = savedKey.isNotBlank()

    Column(
        modifier = if (p.isNeoBrutalist) {
            Modifier
                .fillMaxWidth()
                .neoBrutalist(
                    backgroundColor = p.card,
                    borderColor = p.border,
                    shadowColor = NeoBrutalistColors.Black,
                    borderWidth = 2.5.dp,
                    shadowOffset = 4.dp,
                    cornerRadius = 8.dp
                )
                .padding(16.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(20.dp),
                    tint = p.card,
                    blurRadius = 12.dp,
                    refractionHeight = 8.dp,
                    refractionAmount = 14.dp,
                    chromaticAberration = false
                )
                .padding(16.dp)
        },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = if (p.isNeoBrutalist) FontWeight.ExtraBold else FontWeight.Bold,
            color = p.text
        )
        Text(hint, fontSize = 12.sp, color = p.muted)

        SField(
            value = input,
            onChange = {
                input = it
                // Reveal while typing so the user can verify, hide again on save.
                if (!revealed && saved && it != savedKey) revealed = true
            },
            placeholder = if (saved && !revealed) "••••••••••••••••••••" else placeholder,
            visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                if (p.isNeoBrutalist) {
                    NeoBrutalistBox(
                        onClick = { revealed = !revealed },
                        role = Role.Button,
                        backgroundColor = NeoBrutalistColors.OffWhite,
                        borderColor = p.border,
                        shadowColor = NeoBrutalistColors.Black,
                        borderWidth = 1.5.dp,
                        shadowOffset = 2.dp,
                        cornerRadius = 6.dp,
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (revealed) R.string.api_key_hide else R.string.api_key_show
                            ),
                            tint = NeoBrutalistColors.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { revealed = !revealed },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (revealed) R.string.api_key_hide else R.string.api_key_show
                            ),
                            tint = p.muted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        )

        if (saved) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = null,
                    tint = p.success,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = stringResource(
                        if (revealed) R.string.api_key_visible else R.string.api_key_saved_hint
                    ),
                    fontSize = 11.sp,
                    fontFamily = if (p.isNeoBrutalist) FontFamily.Monospace else FontFamily.Default,
                    fontWeight = if (p.isNeoBrutalist) FontWeight.Bold else FontWeight.Normal,
                    color = p.muted
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (p.isNeoBrutalist) {
                NeoBrutalistBox(
                    onClick = {
                        onSave(input)
                        revealed = false
                    },
                    role = Role.Button,
                    backgroundColor = NeoBrutalistColors.Yellow,
                    borderColor = p.border,
                    shadowColor = NeoBrutalistColors.Black,
                    borderWidth = 2.dp,
                    shadowOffset = 3.dp,
                    cornerRadius = 6.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        saveLabel,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeoBrutalistColors.Black
                    )
                }
                if (saved) {
                    NeoBrutalistBox(
                        onClick = {
                            onClear()
                            input = ""
                            revealed = true
                        },
                        role = Role.Button,
                        backgroundColor = NeoBrutalistColors.CoralRed,
                        borderColor = p.border,
                        shadowColor = NeoBrutalistColors.Black,
                        borderWidth = 2.dp,
                        shadowOffset = 3.dp,
                        cornerRadius = 6.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            stringResource(R.string.delete),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBrutalistColors.Black
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(p.primary)
                        .clickable {
                            onSave(input)
                            revealed = false
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(saveLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = p.onAccent)
                }
                if (saved) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(p.chip)
                            .clickable {
                                onClear()
                                input = ""
                                revealed = true
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(stringResource(R.string.delete), fontSize = 12.sp, color = p.muted)
                    }
                }
            }
        }
    }
}
