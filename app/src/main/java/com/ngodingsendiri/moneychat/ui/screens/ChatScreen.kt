package com.ngodingsendiri.moneychat.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ngodingsendiri.moneychat.R
import com.ngodingsendiri.moneychat.data.local.ChatMessage
import com.ngodingsendiri.moneychat.data.remote.ImageFileUtil
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ngodingsendiri.moneychat.ui.theme.AiPurple
import com.ngodingsendiri.moneychat.ui.theme.AiPurpleLight
import com.ngodingsendiri.moneychat.ui.theme.ExpenseRed
import com.ngodingsendiri.moneychat.ui.theme.ExpenseRedLight
import com.ngodingsendiri.moneychat.ui.theme.IncomeGreen
import com.ngodingsendiri.moneychat.ui.theme.IncomeGreenLight
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val MAX_MESSAGE_LENGTH = 2000

/** Baris chat: pemisah tanggal atau pesan (dengan flag header grup pengirim). */
private sealed interface ChatRow {
    data class Header(val label: String, val key: String) : ChatRow
    data class MessageRow(val message: ChatMessage, val showSenderHeader: Boolean) : ChatRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    activeSender: String,
    isAiThinking: Boolean,
    quickSuggestions: List<String>,
    onSendMessage: (String, String?, String?, String?, String?, String?) -> Unit,
    onEditMessage: (Long, String) -> Unit,
    onAskAiClicked: (String) -> Unit,
    onDeleteMessage: (Long) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var inputText by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<ChatMessage?>(null) }
    var pendingImagePath by remember { mutableStateOf<String?>(null) }
    var pendingFilePath by remember { mutableStateOf<String?>(null) }
    var pendingFileName by remember { mutableStateOf<String?>(null) }
    var replyTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    var attachMenuOpen by remember { mutableStateOf(false) }

    // Smooth color transitions instead of instant snapping
    val sendBgColor by animateColorAsState(
        targetValue = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "sendBg"
    )
    val sendTintColor by animateColorAsState(
        targetValue = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "sendTint"
    )
    val askAiTint by animateColorAsState(
        targetValue = when {
            inputText.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            isDark -> Color(0xFFD0BCFF)
            else -> AiPurple
        },
        animationSpec = tween(200),
        label = "askAiTint"
    )

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var lastKnownCount by remember { mutableIntStateOf(-1) }

    val context = LocalContext.current
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraTempUri
        cameraTempUri = null
        if (success && uri != null) {
            coroutineScope.launch {
                pendingImagePath = ImageFileUtil.saveImageFromUri(context, uri)
            }
        }
    }
    val pickGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                pendingImagePath = ImageFileUtil.saveImageFromUri(context, uri)
            }
        }
    }
    val pickPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val saved = ImageFileUtil.saveFileFromUri(context, uri)
                if (saved != null) {
                    pendingFilePath = saved.path
                    pendingFileName = saved.name
                }
            }
        }
    }

    val todayLabel = stringResource(R.string.today_label)
    val yesterdayLabel = stringResource(R.string.yesterday_label)
    val rows = remember(messages, todayLabel, yesterdayLabel) {
        buildChatRows(messages, todayLabel, yesterdayLabel)
    }

    // Tombol "lompat ke pesan terbaru" muncul saat user tidak di dasar obrolan.
    val shouldShowJumpButton by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && lastVisible < info.totalItemsCount - 4
        }
    }

    // Auto-scroll: halus kalau sudah di bawah & ada konten baru; instan saat pertama
    // dibuka; TIDAK menarik user yang sedang membaca riwayat di atas.
    LaunchedEffect(rows.size, isAiThinking) {
        if (rows.isNotEmpty()) {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val nearBottom = info.totalItemsCount == 0 || lastVisible >= info.totalItemsCount - 4
            if (lastKnownCount >= 0) {
                if (nearBottom) listState.animateScrollToItem(rows.size - 1)
            } else {
                listState.scrollToItem(rows.size - 1)
            }
            lastKnownCount = rows.size
        }
    }

    val sendMessage = {
        val text = inputText.trim()
        val image = pendingImagePath
        val file = pendingFilePath
        val fileName = pendingFileName
        if (text.isNotBlank() || image != null || file != null) {
            onSendMessage(
                text, image, file, fileName,
                replyTarget?.sender, replyTarget?.messageText
            )
            inputText = ""
            pendingImagePath = null
            pendingFilePath = null
            pendingFileName = null
            replyTarget = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chat Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty() && !isAiThinking) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.chat_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.chat_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                items(rows, key = { row ->
                    when (row) {
                        is ChatRow.Header -> row.key
                        is ChatRow.MessageRow -> "msg_${row.message.id}"
                    }
                }) { row ->
                    when (row) {
                        is ChatRow.Header -> DateSeparator(label = row.label)

                        is ChatRow.MessageRow -> {
                            var menuOpen by remember { mutableStateOf(false) }
                            val clipboard = LocalClipboardManager.current
                            val msg = row.message
                            Box(modifier = Modifier.fillMaxWidth()) {
                                ChatMessageBubble(
                                    message = msg,
                                    currentActiveSender = activeSender,
                                    showHeader = row.showSenderHeader,
                                    onLongPress = { menuOpen = true },
                                    onReply = { replyTarget = msg },
                                    onOpenFile = { openAttachedFile(context, msg) },
                                    modifier = Modifier.animateItem()
                                )
                                DropdownMenu(
                                    expanded = menuOpen,
                                    onDismissRequest = { menuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_reply)) },
                                        leadingIcon = { Icon(Icons.Rounded.Reply, contentDescription = null) },
                                        onClick = {
                                            replyTarget = msg
                                            menuOpen = false
                                        }
                                    )
                                    if (msg.sender == activeSender && msg.sender != "AI") {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.chat_edit)) },
                                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                            onClick = {
                                                editingMessage = msg
                                                menuOpen = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_copy)) },
                                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                                        onClick = {
                                            clipboard.setText(AnnotatedString(msg.messageText))
                                            menuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_delete)) },
                                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                        onClick = {
                                            pendingDelete = msg
                                            menuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (isAiThinking) {
                    item {
                        AiThinkingBubble(modifier = Modifier.animateItem())
                    }
                }
            }

            // Quick Suggestion Chips (placed above input field)
            AnimatedVisibility(
                visible = inputText.isBlank() && quickSuggestions.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(240)) + fadeIn(animationSpec = tween(240)),
                exit = slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
            ) {
                QuickSuggestionRow(
                    suggestions = quickSuggestions,
                    onSuggestionClicked = { inputText = it }
                )
            }

            // Bar balasan (reply) — muncul saat user membalas pesan via swipe/menu
            AnimatedVisibility(
                visible = replyTarget != null,
                enter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(240)) + fadeIn(animationSpec = tween(240)),
                exit = slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
            ) {
                val target = replyTarget
                if (target != null) {
                    val snippet = target.messageText.ifBlank {
                        target.fileName ?: target.imagePath?.let { "📷" } ?: ""
                    }
                    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Reply,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.chat_reply_label, target.sender),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = snippet,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { replyTarget = null }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.chat_reply_cancel),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Pratinjau dokumen (PDF) sebelum dikirim
            AnimatedVisibility(
                visible = pendingFilePath != null,
                enter = fadeIn(animationSpec = tween(200)) + slideInVertically(initialOffsetY = { it }, animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150))
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = pendingFileName ?: stringResource(R.string.chat_pdf_attached),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { pendingFilePath = null; pendingFileName = null }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.chat_image_remove)
                            )
                        }
                    }
                }
            }

            // Pratinjau foto lampiran (nota belanja) sebelum dikirim
            val previewPath = pendingImagePath
            val previewBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = previewPath
            ) {
                value = withContext(Dispatchers.IO) {
                    previewPath?.let { ImageFileUtil.decodeImage(it, 640) }
                }
            }
            AnimatedVisibility(
                visible = previewPath != null,
                enter = fadeIn(animationSpec = tween(200)) + slideInVertically(initialOffsetY = { it }, animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150))
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        previewBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = stringResource(R.string.chat_image_desc),
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.chat_image_attached),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { pendingImagePath = null }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.chat_image_remove)
                            )
                        }
                    }
                }
            }

            // Chat Input Box
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box {
                        IconButton(onClick = { attachMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.chat_attach_desc),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = attachMenuOpen,
                            onDismissRequest = { attachMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_take_photo)) },
                                leadingIcon = { Icon(Icons.Rounded.PhotoCamera, contentDescription = null) },
                                onClick = {
                                    attachMenuOpen = false
                                    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                                    val file = File(dir, "cam_${System.currentTimeMillis()}.jpg")
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    cameraTempUri = uri
                                    runCatching { takePictureLauncher.launch(uri) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_pick_gallery)) },
                                leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null) },
                                onClick = {
                                    attachMenuOpen = false
                                    pickGalleryLauncher.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_send_pdf)) },
                                leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    attachMenuOpen = false
                                    pickPdfLauncher.launch(arrayOf("application/pdf"))
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) inputText = it },
                        placeholder = {
                            Text(stringResource(R.string.chat_input_placeholder))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        maxLines = 4,
                        minLines = 1,
                        trailingIcon = {
                            IconButton(
                                enabled = inputText.isNotBlank(),
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        onAskAiClicked(inputText)
                                        inputText = ""
                                    }
                                },
                                modifier = Modifier.testTag("ask_ai_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = stringResource(R.string.chat_ask_ai_desc),
                                    tint = askAiTint
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        IconButton(
                            enabled = inputText.isNotBlank() || pendingImagePath != null || pendingFilePath != null,
                            onClick = sendMessage,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(sendBgColor)
                                .testTag("send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.chat_send_desc),
                                tint = sendTintColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Tombol lompat ke pesan terbaru (muncul saat scroll ke atas)
        AnimatedVisibility(
            visible = shouldShowJumpButton,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp),
            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(150))
        ) {
            FloatingActionButton(
                onClick = { coroutineScope.launch { listState.animateScrollToItem(rows.size - 1) } },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("jump_to_bottom")
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.chat_jump_bottom_desc)
                )
            }
        }

        // Konfirmasi hapus pesan
        pendingDelete?.let { msg ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(stringResource(R.string.confirm_delete_message_title)) },
                text = { Text(stringResource(R.string.confirm_delete_message_text)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteMessage(msg.id)
                            pendingDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // Dialog edit pesan
        editingMessage?.let { msg ->
            var editText by remember(msg.id) { mutableStateOf(msg.messageText) }
            AlertDialog(
                onDismissRequest = { editingMessage = null },
                title = { Text(stringResource(R.string.chat_edit_title)) },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.chat_edit_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editText,
                            onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) editText = it },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 5
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = editText.isNotBlank(),
                        onClick = {
                            onEditMessage(msg.id, editText)
                            editingMessage = null
                        }
                    ) {
                        Text(stringResource(R.string.chat_save_edit))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingMessage = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

/** Buka file dokumen terkirim (PDF/invoice) lewat aplikasi pembaca eksternal. */
private fun openAttachedFile(context: Context, message: ChatMessage) {
    val path = message.filePath ?: return
    val file = File(path)
    if (!file.exists()) return
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.chat_file_open_failed), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun DateSeparator(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    currentActiveSender: String,
    showHeader: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    onReply: (() -> Unit)? = null,
    onOpenFile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isAi = message.sender == "AI"
    val isMe = message.sender == currentActiveSender
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val alignment = when {
        isAi -> Alignment.Start
        isMe -> Alignment.End
        else -> Alignment.Start
    }

    // Warna bubble lebih lembut & konsisten dengan tema (container tones)
    val bubbleColor = when {
        isAi -> if (isDark) Color(0xFF2A2140) else AiPurpleLight
        isMe -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        isMe -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val timeColor = when {
        isMe -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    val senderLabel = when (message.sender) {
        "Bendahara" -> stringResource(R.string.sender_bendahara)
        "Anggota" -> stringResource(R.string.sender_anggota)
        "Ketua" -> stringResource(R.string.sender_ketua)
        "AI" -> stringResource(R.string.sender_ai)
        else -> message.sender
    }

    val senderColor = when {
        isMe -> MaterialTheme.colorScheme.primary
        isAi -> if (isDark) Color(0xFFD0BCFF) else AiPurple
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID")) }
    val formattedTime = timeFormat.format(Date(message.timestamp))
    // Penanda pesan pernah diedit (mis. "14:05 • diedit")
    val timeDisplay = if (message.editedAt != null) {
        "$formattedTime • ${stringResource(R.string.chat_edited)}"
    } else formattedTime

    // Dekode foto lampiran untuk ditampilkan di bubble (disampling, aman memori)
    val imagePath = message.imagePath
    val imageBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = imagePath
    ) {
        value = withContext(Dispatchers.IO) {
            imagePath?.let { ImageFileUtil.decodeImage(it, 1100) }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isMe && showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
            ) {
                // Avatar inisial pengirim
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(senderColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = senderLabel.take(1).uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = senderColor
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = senderLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = senderColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isMe) 20.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 20.dp
            ),
            color = bubbleColor,
            shadowElevation = if (isMe) 0.dp else 1.dp,
            modifier = Modifier
                .widthIn(min = 60.dp, max = 300.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { onLongPress?.invoke() }
                )
                .then(
                    if (onReply != null) {
                        // Geser bubble ke kanan untuk membalas (pola app chat modern)
                        Modifier.pointerInput(Unit) {
                            var total = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { total = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    total += dragAmount
                                    change.consume()
                                },
                                onDragEnd = { if (total > 70.dp.toPx()) onReply() }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .testTag("chat_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                // Kutipan pesan yang dibalas (swipe kanan / menu Balas)
                message.replyToText?.takeIf { it.isNotBlank() }?.let { quoted ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bubbleColor.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = message.replyToSender ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = senderColor
                            )
                            Text(
                                text = quoted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = textColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // File dokumen (PDF/invoice/nota) — ketuk untuk membuka
                if (message.filePath != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMe) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .widthIn(max = 230.dp)
                            .combinedClickable(onClick = { onOpenFile?.invoke() })
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                tint = if (isMe) Color.White else ExpenseRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.fileName ?: stringResource(R.string.chat_pdf_attached),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.chat_pdf_attached),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = timeColor
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Foto lampiran (nota belanja) — proporsional, tidak memenuhi lebar chat
                imageBitmap?.let { b ->
                    Image(
                        bitmap = b.asImageBitmap(),
                        contentDescription = stringResource(R.string.chat_image_desc),
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    if (message.messageText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (message.messageText.isNotBlank()) {
                    Text(
                        text = message.messageText,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = textColor
                    )
                }

                if (isMe) {
                    Text(
                        text = timeDisplay,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = timeColor,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }

                // Financial Tag Badge inside message
                if (message.isFinancial && message.detectedAmount != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val isIncome = message.detectedType == "PEMASUKAN"
                    val tagBg = if (isIncome) if (isDark) Color(0xFF0F5223) else IncomeGreenLight else if (isDark) Color(0xFF8C1D18) else ExpenseRedLight
                    val tagColor = if (isIncome) if (isDark) Color(0xFFC4EED0) else IncomeGreen else if (isDark) Color(0xFFF9DEDC) else ExpenseRed

                    val formatRp = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
                        maximumFractionDigits = 0
                    }.format(message.detectedAmount)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = tagBg,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isIncome) Icons.Rounded.CheckCircle else Icons.Rounded.Receipt,
                                contentDescription = null,
                                tint = tagColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${if (isIncome) "+ " else "- "}$formatRp (${message.detectedCategory})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tagColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiThinkingBubble(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF331650) else AiPurpleLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFFD0BCFF) else AiPurple.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = if (isDark) Color(0xFFD0BCFF) else AiPurple,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.chat_ai_thinking),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) Color(0xFFD0BCFF) else AiPurple,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun QuickSuggestionRow(
    suggestions: List<String>,
    onSuggestionClicked: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { text ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onSuggestionClicked(text) }
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---- Helpers pembangun baris chat ----

private fun buildChatRows(
    messages: List<ChatMessage>,
    todayLabel: String,
    yesterdayLabel: String
): List<ChatRow> {
    val rows = mutableListOf<ChatRow>()
    messages.forEachIndexed { index, msg ->
        val prev = messages.getOrNull(index - 1)
        if (prev == null || !isSameDay(prev.timestamp, msg.timestamp)) {
            rows.add(
                ChatRow.Header(
                    label = dayLabel(msg.timestamp, todayLabel, yesterdayLabel),
                    key = "day_${msg.timestamp}"
                )
            )
        }
        rows.add(ChatRow.MessageRow(msg, prev?.sender != msg.sender))
    }
    return rows
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private fun dayLabel(timestamp: Long, todayLabel: String, yesterdayLabel: String): String {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val msgDay = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return when (msgDay.timeInMillis) {
        today.timeInMillis -> todayLabel
        today.timeInMillis - 86_400_000L -> yesterdayLabel
        else -> SimpleDateFormat("EEEE, dd MMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(timestamp))
    }
}
