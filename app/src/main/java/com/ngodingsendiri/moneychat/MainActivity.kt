package com.ngodingsendiri.moneychat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.FileProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ngodingsendiri.moneychat.R
import com.ngodingsendiri.moneychat.data.backup.DriveBackupFile
import com.ngodingsendiri.moneychat.data.backup.DriveBackupManager
import com.ngodingsendiri.moneychat.data.backup.DriveConsentRequired
import com.ngodingsendiri.moneychat.ui.MainViewModel
import com.ngodingsendiri.moneychat.ui.screens.AddTransactionDialog
import com.ngodingsendiri.moneychat.ui.screens.AiReportDialog
import com.ngodingsendiri.moneychat.ui.screens.ChatScreen
import com.ngodingsendiri.moneychat.ui.screens.RekapScreen
import com.ngodingsendiri.moneychat.data.remote.GitHubRelease
import com.ngodingsendiri.moneychat.data.remote.GitHubUpdateChecker
import com.ngodingsendiri.moneychat.ui.theme.CoupleFinanceTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = remember {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean("is_dark_mode", false)) }

            CoupleFinanceTheme(darkTheme = isDarkMode) {
                val messages by viewModel.messages.collectAsStateWithLifecycle()
                val transactions by viewModel.transactions.collectAsStateWithLifecycle()
                val activeSender by viewModel.activeSender.collectAsStateWithLifecycle()
                val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
                val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
                val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
                val auditReport by viewModel.auditReport.collectAsStateWithLifecycle()
                val isAuditLoading by viewModel.isAuditLoading.collectAsStateWithLifecycle()
                val quickSuggestions by viewModel.quickSuggestions.collectAsStateWithLifecycle()

                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var showAddDialog by remember { mutableStateOf(false) }
                var showSettingsMenu by remember { mutableStateOf(false) }
                var showGeminiKeyDialog by remember { mutableStateOf(false) }
                var showOpenRouterKeyDialog by remember { mutableStateOf(false) }
                var showConfirmClearDialog by remember { mutableStateOf(false) }

                var workspacePin by remember { mutableStateOf(prefs.getString("workspace_pin", null)) }
                var userName by remember { mutableStateOf(prefs.getString("user_name", null)) }
                var firebaseReady by remember { mutableStateOf(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) }
                val scope = rememberCoroutineScope()
                var updateInfo by remember { mutableStateOf<GitHubRelease?>(null) }
                var isDownloadingUpdate by remember { mutableStateOf(false) }
                var updateMessage by remember { mutableStateOf<String?>(null) }
                var geminiKey by remember { mutableStateOf(prefs.getString("gemini_api_key", null)) }
                var openRouterKey by remember { mutableStateOf(prefs.getString("openrouter_api_key", null)) }

                // ---- State Export CSV & Backup Google Drive ----
                var backupBusy by remember { mutableStateOf(false) }
                var backupMessage by remember { mutableStateOf<String?>(null) }
                var driveConsentIntent by remember { mutableStateOf<Intent?>(null) }
                var pendingDriveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
                var restoreBackups by remember { mutableStateOf<List<DriveBackupFile>?>(null) }
                var restoreTarget by remember { mutableStateOf<DriveBackupFile?>(null) }

                LaunchedEffect(workspacePin, userName) {
                    val pin = workspacePin
                    if (pin != null) {
                        viewModel.startCloudSync(pin)
                    } else {
                        viewModel.stopCloudSync()
                    }
                    userName?.let { viewModel.setSender(it) }
                }
                LaunchedEffect(geminiKey) {
                    com.ngodingsendiri.moneychat.data.remote.GeminiService.userApiKey = geminiKey
                }
                LaunchedEffect(openRouterKey) {
                    com.ngodingsendiri.moneychat.data.remote.OpenRouterService.userApiKey = openRouterKey
                }
                LaunchedEffect(Unit) {
                    // Cek update otomatis (throttle 1 jam biar gak nembak GitHub API tiap buka app).
                    // Timestamp cuma di-set kalau ceknya SUKSES — kalau gagal (offline/rate-limit),
                    // cooldown tidak terpakai dan dicoba lagi saat app dibuka berikutnya.
                    val lastCheck = prefs.getLong("last_update_check", 0L)
                    if (System.currentTimeMillis() - lastCheck > 60 * 60 * 1000L) {
                        val release = GitHubUpdateChecker.checkLatest()
                        if (release != null) {
                            prefs.edit().putLong("last_update_check", System.currentTimeMillis()).apply()
                            if (GitHubUpdateChecker.isNewer(release.versionName, BuildConfig.VERSION_NAME)) {
                                updateInfo = release
                            }
                        }
                    }
                }

                // Launcher konsen OAuth Drive (muncul sekali; setelah disetujui
                // aksi diulang otomatis). Kalau user menekan Batal (bukan OK),
                // aksi tidak diulang supaya tidak muncul dialog berulang-ulang.
                val consentLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == android.app.Activity.RESULT_OK) {
                        pendingDriveAction?.invoke()
                    } else {
                        backupMessage = context.getString(R.string.drive_consent_cancelled)
                    }
                    pendingDriveAction = null
                }
                LaunchedEffect(driveConsentIntent) {
                    driveConsentIntent?.let {
                        driveConsentIntent = null
                        consentLauncher.launch(it)
                    }
                }

                // Launcher simpan CSV via Storage Access Framework (pilih folder, biasanya Download)
                val exportCsvLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("text/csv")
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            val csv = viewModel.exportRecapCsv()
                            val ok = runCatching {
                                context.contentResolver.openOutputStream(uri)?.use { out ->
                                    out.write(csv.toByteArray(Charsets.UTF_8))
                                }
                            }.isSuccess
                            backupMessage = context.getString(
                                if (ok) R.string.export_csv_success else R.string.export_csv_failed
                            )
                        }
                    }
                }

                // Ambil token Drive; kalau butuh persetujuan, tampilkan Intent lalu ulangi aksinya.
                suspend fun driveToken(context: Context, retry: () -> Unit): String? {
                    val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
                    if (email == null) {
                        backupMessage = context.getString(R.string.drive_err_not_signed_in)
                        return null
                    }
                    return try {
                        DriveBackupManager.getAccessToken(context, email)
                    } catch (e: DriveConsentRequired) {
                        driveConsentIntent = e.intent
                        pendingDriveAction = retry
                        null
                    } catch (e: Exception) {
                        backupMessage = context.getString(
                            R.string.drive_err_token, e.message ?: e.javaClass.simpleName
                        )
                        null
                    }
                }

                fun startDriveBackup() {
                    scope.launch {
                        backupBusy = true
                        try {
                            val token = driveToken(context) { startDriveBackup() } ?: return@launch
                            val json = viewModel.buildBackupJson()
                            val fileName = "MoneyChat-backup-${timestampForFile()}.json"
                            val ok = DriveBackupManager.uploadBackup(context, token, fileName, json)
                            if (ok) {
                                DriveBackupManager.pruneOldBackups(context, token, 5)
                                backupMessage = context.getString(R.string.backup_success, fileName)
                            } else {
                                backupMessage = context.getString(R.string.backup_failed)
                            }
                        } finally {
                            backupBusy = false
                        }
                    }
                }

                fun startDriveRestore() {
                    scope.launch {
                        backupBusy = true
                        try {
                            val token = driveToken(context) { startDriveRestore() } ?: return@launch
                            val files = DriveBackupManager.listBackups(context, token)
                            if (files.isEmpty()) {
                                backupMessage = context.getString(R.string.restore_no_backup)
                            } else {
                                restoreBackups = files.take(5)
                            }
                        } finally {
                            backupBusy = false
                        }
                    }
                }

                fun confirmRestore(file: DriveBackupFile) {
                    scope.launch {
                        backupBusy = true
                        try {
                            val token = driveToken(context) { confirmRestore(file) } ?: return@launch
                            val json = DriveBackupManager.downloadBackup(context, token, file.fileId)
                            if (json == null) {
                                backupMessage = context.getString(R.string.restore_failed)
                                return@launch
                            }
                            val ok = viewModel.restoreFromJson(json)
                            backupMessage = context.getString(
                                if (ok) R.string.restore_success else R.string.restore_failed_parse
                            )
                        } finally {
                            backupBusy = false
                            restoreBackups = null
                            restoreTarget = null
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    GlowingBackground()

                    if (workspacePin == null || userName == null || !firebaseReady) {
                        com.ngodingsendiri.moneychat.ui.screens.PinConnectScreen(
                            onPinConnected = { pin, name ->
                                firebaseReady = true
                                prefs.edit()
                                    .putString("workspace_pin", pin)
                                    .putString("user_name", name)
                                    .apply()
                                workspacePin = pin
                                userName = name
                                viewModel.setSender(name)
                            }
                        )
                    } else {
                        Scaffold(
                            containerColor = Color.Transparent,
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Column {
                                            Text(
                                                text = stringResource(R.string.topbar_title),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = stringResource(R.string.topbar_subtitle),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    },
                                    actions = {
                                        Box {
                                            IconButton(onClick = { showSettingsMenu = true }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.MoreVert,
                                                    contentDescription = stringResource(R.string.action_settings),
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showSettingsMenu,
                                                onDismissRequest = { showSettingsMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.menu_version, BuildConfig.VERSION_NAME)) },
                                                    onClick = {},
                                                    enabled = false
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(if (isDarkMode) R.string.menu_mode_light else R.string.menu_mode_dark)) },
                                                    onClick = { 
                                                        isDarkMode = !isDarkMode
                                                        prefs.edit().putBoolean("is_dark_mode", isDarkMode).apply()
                                                        showSettingsMenu = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.menu_gemini_key)) },
                                                    onClick = {
                                                        showSettingsMenu = false
                                                        showGeminiKeyDialog = true
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Key,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.menu_openrouter_key)) },
                                                    onClick = {
                                                        showSettingsMenu = false
                                                        showOpenRouterKeyDialog = true
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Route,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.menu_check_update)) },
                                                    onClick = {
                                                        showSettingsMenu = false
                                                        scope.launch {
                                                            val release = GitHubUpdateChecker.checkLatest()
                                                            if (release != null && GitHubUpdateChecker.isNewer(release.versionName, BuildConfig.VERSION_NAME)) {
                                                                updateInfo = release
                                                            } else {
                                                                updateMessage = context.getString(R.string.update_no_update)
                                                            }
                                                        }
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.SystemUpdate,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.menu_export_csv)) },
                                                    onClick = {
                                                        showSettingsMenu = false
                                                        exportCsvLauncher.launch(
                                                            "MoneyChat-rekap-${timestampForFile()}.csv"
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.TableChart,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.menu_backup_drive)) },
                                                    onClick = {
                                                        showSettingsMenu = false
                                                        startDriveBackup()
                                                    },
                                                    enabled = !backupBusy,
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.CloudUpload,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.menu_restore_drive)) },
                                                    onClick = {
                                                        showSettingsMenu = false
                                                        startDriveRestore()
                                                    },
                                                    enabled = !backupBusy,
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.CloudDownload,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.menu_clear_data)) },
                                                    onClick = { 
                                                        showSettingsMenu = false
                                                        showConfirmClearDialog = true
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.menu_logout, userName ?: "User")) },
                                                    onClick = { 
                                                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                                        firebaseReady = false
                                                        prefs.edit().clear().apply()
                                                        workspacePin = null
                                                        userName = null
                                                        geminiKey = null
                                                        openRouterKey = null
                                                        showSettingsMenu = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            },
                            bottomBar = {
                                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)) {
                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        icon = {
                                            Icon(
                                                imageVector = if (selectedTab == 0) Icons.Rounded.ChatBubble else Icons.Rounded.ChatBubbleOutline,
                                                contentDescription = stringResource(R.string.tab_chat_desc)
                                            )
                                        },
                                        label = { Text(stringResource(R.string.tab_diskusi)) },
                                        modifier = Modifier.testTag("tab_chat")
                                    )

                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Rounded.PieChart,
                                                contentDescription = stringResource(R.string.tab_rekap_desc)
                                            )
                                        },
                                        label = { Text(stringResource(R.string.tab_rekap)) },
                                        modifier = Modifier.testTag("tab_rekap")
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                AnimatedContent(
                                    targetState = selectedTab,
                                    transitionSpec = {
                                        val forward = targetState > initialState
                                        val enter = slideInHorizontally(
                                            initialOffsetX = { if (forward) it / 5 else -it / 5 },
                                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                                        ) + fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
                                        val exit = slideOutHorizontally(
                                            targetOffsetX = { if (forward) -it / 5 else it / 5 },
                                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                                        ) + fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
                                        enter togetherWith exit
                                    },
                                    label = "tabContent"
                                ) { tab ->
                                    when (tab) {
                                        0 -> ChatScreen(
                                            quickSuggestions = quickSuggestions,
                                            messages = messages,
                                            activeSender = activeSender,
                                            isAiThinking = isAiThinking,
                                            onSendMessage = { text, imagePath -> viewModel.sendMessage(text, imagePath) },
                                            onAskAiClicked = { viewModel.askAiInChat(it) },
                                            onDeleteMessage = { viewModel.deleteChatMessage(it) }
                                        )

                                        1 -> RekapScreen(
                                            transactions = transactions,
                                            totalIncome = totalIncome,
                                            totalExpense = totalExpense,
                                            isAuditLoading = isAuditLoading,
                                            onGenerateAudit = { viewModel.generateAiAuditReport() },
                                            onAddTransactionClicked = { showAddDialog = true },
                                            onDeleteTransaction = { viewModel.deleteTransaction(it) }
                                        )
                                    }
                                }
                            }

                            // Dialogs
                            if (showAddDialog) {
                                AddTransactionDialog(
                                    onDismiss = { showAddDialog = false },
                                    onConfirm = { tx ->
                                        viewModel.addManualTransaction(tx.type, tx.category, tx.amount, tx.description, tx.loggedBy)
                                    }
                                )
                            }

                            if (showGeminiKeyDialog) {
                                ApiKeyDialog(
                                    title = stringResource(R.string.menu_gemini_key),
                                    hint = stringResource(R.string.gemini_key_hint),
                                    initialKey = geminiKey ?: "",
                                    onDismiss = { showGeminiKeyDialog = false },
                                    onSave = { newKey ->
                                        prefs.edit().putString("gemini_api_key", newKey).apply()
                                        geminiKey = newKey
                                        showGeminiKeyDialog = false
                                    }
                                )
                            }

                            if (showOpenRouterKeyDialog) {
                                ApiKeyDialog(
                                    title = stringResource(R.string.menu_openrouter_key),
                                    hint = stringResource(R.string.openrouter_key_hint),
                                    initialKey = openRouterKey ?: "",
                                    onDismiss = { showOpenRouterKeyDialog = false },
                                    onSave = { newKey ->
                                        prefs.edit().putString("openrouter_api_key", newKey).apply()
                                        openRouterKey = newKey
                                        showOpenRouterKeyDialog = false
                                    }
                                )
                            }

                            if (showConfirmClearDialog) {
                                AlertDialog(
                                    onDismissRequest = { showConfirmClearDialog = false },
                                    title = { Text(stringResource(R.string.confirm_clear_title)) },
                                    text = {
                                        Text(stringResource(R.string.confirm_clear_message))
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                viewModel.clearAllData()
                                                showConfirmClearDialog = false
                                            }
                                        ) {
                                            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showConfirmClearDialog = false }) {
                                            Text(stringResource(R.string.action_cancel))
                                        }
                                    }
                                )
                            }

                            auditReport?.let { report ->
                                AiReportDialog(
                                    reportText = report,
                                    onDismiss = { viewModel.dismissAuditReport() }
                                )
                            }
                        }
                    }

                    // Dialog update tampil di SEMUA layar (termasuk layar login/PIN),
                    // jadi yang belum selesai onboarding tetap dapat notif rilis baru.
                    updateInfo?.let { release ->
                        AlertDialog(
                            onDismissRequest = { if (!isDownloadingUpdate) updateInfo = null },
                            icon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) },
                            title = { Text(stringResource(R.string.update_available_title)) },
                            text = {
                                Text(
                                    text = if (isDownloadingUpdate) {
                                        stringResource(R.string.update_downloading)
                                    } else {
                                        stringResource(R.string.update_available_message, release.versionName)
                                    }
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    enabled = !isDownloadingUpdate,
                                    onClick = {
                                        scope.launch {
                                            isDownloadingUpdate = true
                                            try {
                                                val url = release.apkUrl
                                                if (url == null) throw IllegalStateException("APK tidak tersedia di release")
                                                val dest = File(context.cacheDir, "downloads/moneychat-${release.versionName}.apk")
                                                GitHubUpdateChecker.downloadApk(url, dest)
                                                installApk(context, dest)
                                            } catch (e: Exception) {
                                                updateMessage = context.getString(R.string.update_download_failed)
                                            } finally {
                                                isDownloadingUpdate = false
                                                updateInfo = null
                                            }
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.update_action))
                                }
                            },
                            dismissButton = {
                                if (!isDownloadingUpdate) {
                                    TextButton(onClick = { updateInfo = null }) {
                                        Text(stringResource(R.string.update_later))
                                    }
                                }
                            }
                        )
                    }

                    updateMessage?.let { msg ->
                        AlertDialog(
                            onDismissRequest = { updateMessage = null },
                            title = { Text(stringResource(R.string.update_check_title)) },
                            text = { Text(msg) },
                            confirmButton = {
                                TextButton(onClick = { updateMessage = null }) {
                                    Text(stringResource(R.string.action_ok))
                                }
                            }
                        )
                    }

                    // ---- Dialog Export CSV / Backup / Restore Drive ----
                    if (backupBusy) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text(stringResource(R.string.backup_progress)) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        stringResource(R.string.backup_please_wait),
                                        fontSize = 13.5.sp
                                    )
                                }
                            },
                            confirmButton = {}
                        )
                    }

                    backupMessage?.let { msg ->
                        AlertDialog(
                            onDismissRequest = { backupMessage = null },
                            title = { Text(stringResource(R.string.backup_info_title)) },
                            text = { Text(msg) },
                            confirmButton = {
                                TextButton(onClick = { backupMessage = null }) {
                                    Text(stringResource(R.string.action_ok))
                                }
                            }
                        )
                    }

                    restoreBackups?.let { files ->
                        AlertDialog(
                            onDismissRequest = { restoreBackups = null },
                            title = { Text(stringResource(R.string.restore_pick_title)) },
                            text = {
                                Column {
                                    Text(
                                        stringResource(R.string.restore_pick_hint),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    files.forEach { f ->
                                        TextButton(
                                            onClick = { restoreTarget = f },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = f.name,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { restoreBackups = null }) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                            }
                        )
                    }

                    restoreTarget?.let { f ->
                        AlertDialog(
                            onDismissRequest = { restoreTarget = null },
                            title = { Text(stringResource(R.string.restore_confirm_title)) },
                            text = { Text(stringResource(R.string.restore_confirm_message, f.name)) },
                            confirmButton = {
                                TextButton(onClick = { confirmRestore(f) }) {
                                    Text(stringResource(R.string.action_restore))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { restoreTarget = null }) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlowingBackground() {
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
    val tertiary = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-100).dp)
                .size(300.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(primary, Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(350.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(secondary, Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 50.dp, y = (-50).dp)
                .size(250.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(tertiary, Color.Transparent)))
        )
    }
}

@Composable
fun ApiKeyDialog(
    title: String,
    hint: String,
    initialKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var key by remember { mutableStateOf(initialKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = hint,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(stringResource(R.string.api_key_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(key.trim()) },
                enabled = key.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {                                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** Nama file dengan timestamp: 20260803-143000 */
private fun timestampForFile(): String =
    SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

/** Buka intent install untuk APK hasil unduhan (via FileProvider). */
private fun installApk(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
