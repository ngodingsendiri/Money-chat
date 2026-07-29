package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.AddTransactionDialog
import com.example.ui.screens.AiReportDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.RekapScreen
import com.example.ui.theme.CoupleFinanceTheme
import com.example.ui.theme.IndigoPrimary

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = remember { context.getSharedPreferences("couple_finance_prefs", android.content.Context.MODE_PRIVATE) }
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

                var selectedTab by remember { mutableIntStateOf(0) }
                var showAddDialog by remember { mutableStateOf(false) }
                var showSettingsMenu by remember { mutableStateOf(false) }

                var workspacePin by remember { mutableStateOf(prefs.getString("workspace_pin", null)) }
                var userName by remember { mutableStateOf(prefs.getString("user_name", null)) }

                LaunchedEffect(workspacePin, userName) {
                    workspacePin?.let { com.example.data.remote.FirestoreSyncManager.setWorkspaceId(it) }
                    userName?.let { viewModel.setSender(it) }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    GlowingBackground()

                    if (workspacePin == null || userName == null) {
                        com.example.ui.screens.PinConnectScreen(
                            onPinConnected = { pin, name ->
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
                                                text = "Keuangan Bersama",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = "Pencatatan & Analisis Finansial",
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
                                                    contentDescription = "Pengaturan",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showSettingsMenu,
                                                onDismissRequest = { showSettingsMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(if (isDarkMode) "Mode Terang" else "Mode Gelap") },
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
                                                    text = { Text("Hapus Semua Data") },
                                                    onClick = { 
                                                        viewModel.clearAllData()
                                                        showSettingsMenu = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Logout (" + (userName ?: "User") + ")") },
                                                    onClick = { 
                                                        prefs.edit().clear().apply()
                                                        workspacePin = null
                                                        userName = null
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
                                                contentDescription = "Chat"
                                            )
                                        },
                                        label = { Text("Diskusi") },
                                        modifier = Modifier.testTag("tab_chat")
                                    )

                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Rounded.PieChart,
                                                contentDescription = "Rekap"
                                            )
                                        },
                                        label = { Text("Rekap") },
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
                                when (selectedTab) {
                                    0 -> ChatScreen(
                                        messages = messages,
                                        activeSender = activeSender,
                                        isAiThinking = isAiThinking,
                                        onSenderChanged = { viewModel.setSender(it) },
                                        onSendMessage = { viewModel.sendMessage(it) },
                                        onAskAiClicked = { viewModel.askAiInChat(it) }
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

                            // Dialogs
                            if (showAddDialog) {
                                AddTransactionDialog(
                                    onDismiss = { showAddDialog = false },
                                    onConfirm = { type, category, amount, description, loggedBy ->
                                        viewModel.addManualTransaction(type, category, amount, description, loggedBy)
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
                .blur(80.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(350.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(secondary, Color.Transparent)))
                .blur(80.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 50.dp, y = (-50).dp)
                .size(250.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(tertiary, Color.Transparent)))
                .blur(80.dp)
        )
    }
}
