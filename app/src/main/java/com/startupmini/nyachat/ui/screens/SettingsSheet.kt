package com.startupmini.nyachat.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startupmini.nyachat.BuildConfig
import com.startupmini.nyachat.R

/**
 * Bottom sheet Pengaturan — di-ekstrak dari MainActivity (P2-13) supaya
 * MainActivity tidak terus membengkak dan tiap aksi bisa diuji berdiri sendiri.
 * Semua aksi (ubah tema, cek update, kelola API key, backup/restore, logout)
 * didelegasikan lewat callback; komponen ini murni tampilan + pemicu aksi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    isDarkMode: Boolean,
    userName: String?,
    backupBusy: Boolean,
    onDismiss: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onCheckUpdate: () -> Unit,
    onGeminiKey: () -> Unit,
    onOpenRouterKey: () -> Unit,
    onPin: () -> Unit,
    onExportCsv: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onClearData: () -> Unit,
    onLogout: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.action_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.menu_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── UMUM ──
            SectionLabel(stringResource(R.string.settings_section_general))
            DropdownMenuItem(
                text = { Text(stringResource(if (isDarkMode) R.string.menu_mode_light else R.string.menu_mode_dark)) },
                onClick = onToggleDarkMode,
                leadingIcon = {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = null
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_check_update)) },
                onClick = onCheckUpdate,
                leadingIcon = { Icon(imageVector = Icons.Rounded.SystemUpdate, contentDescription = null) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // ── AI & API ──
            SectionLabel(stringResource(R.string.settings_section_ai))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_gemini_key)) },
                onClick = onGeminiKey,
                leadingIcon = { Icon(imageVector = Icons.Rounded.Key, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_openrouter_key)) },
                onClick = onOpenRouterKey,
                leadingIcon = { Icon(imageVector = Icons.Rounded.Route, contentDescription = null) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // ── AKUN ──
            SectionLabel(stringResource(R.string.settings_section_account))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_pin)) },
                onClick = onPin,
                leadingIcon = { Icon(imageVector = Icons.Rounded.Pin, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_export_csv)) },
                onClick = onExportCsv,
                leadingIcon = { Icon(imageVector = Icons.Rounded.TableChart, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_backup_drive)) },
                onClick = onBackup,
                enabled = !backupBusy,
                leadingIcon = { Icon(imageVector = Icons.Rounded.CloudUpload, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_restore_drive)) },
                onClick = onRestore,
                enabled = !backupBusy,
                leadingIcon = { Icon(imageVector = Icons.Rounded.CloudDownload, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_clear_data)) },
                onClick = onClearData,
                leadingIcon = { Icon(imageVector = Icons.Rounded.Delete, contentDescription = null) }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.menu_logout, userName ?: "User"),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = onLogout,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
    )
}
