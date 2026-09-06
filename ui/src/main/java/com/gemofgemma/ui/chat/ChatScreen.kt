package com.gemofgemma.ui.chat

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemofgemma.core.model.ChatMessage
import com.gemofgemma.core.model.ToolCategory
import com.gemofgemma.core.model.ToolDefinition
import com.gemofgemma.ui.camera.BoundingBoxOverlay
import com.gemofgemma.ui.components.AnimatedGemIcon
import com.gemofgemma.ui.components.FeatureChip
import com.gemofgemma.ui.components.ThinkingIndicator
import com.gemofgemma.ui.theme.GradientEnd
import com.gemofgemma.ui.theme.GradientStart
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    capturedImageBytes: ByteArray? = null,
    onCapturedImageConsumed: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCapture: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onImagePicked(uri, context.contentResolver)
        }
    }

    // Mic permission
    var pendingMicAction by remember { mutableStateOf(false) }
    val activity = context as? android.app.Activity
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleRecording()
        }
        pendingMicAction = false
    }
    val onMicToggle: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.toggleRecording()
        } else if (activity != null &&
            !activity.shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO)
            && pendingMicAction
        ) {
            // Permission permanently denied — send user to app settings
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", context.packageName, null)
            )
            context.startActivity(intent)
        } else {
            pendingMicAction = true
            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // Handle captured image from navigation
    LaunchedEffect(capturedImageBytes) {
        capturedImageBytes?.let {
            viewModel.attachImage(it)
            onCapturedImageConsumed()
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.streamingText, uiState.thinkingText) {
        if (uiState.messages.isNotEmpty() || uiState.streamingText != null) {
            // Scroll to the very bottom — account for streaming/thinking items after messages
            val extraItems = 1 + // top spacer
                uiState.messages.size +
                (if (uiState.thinkingText != null || uiState.streamingText != null) 1 else 0) +
                (if (uiState.isLoading && uiState.streamingText == null && uiState.thinkingText == null) 1 else 0) +
                1 // bottom spacer
            listState.animateScrollToItem(extraItems - 1)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            keyboardController?.hide()
        }
    }

    pendingConfirmation?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelPendingAction,
            title = { Text("Confirm action") },
            text = { Text(pending.actionDescription) },
            confirmButton = {
                TextButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.confirmPendingAction() }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelPendingAction) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .imePadding()
        ) {
        ChatTopBar(
            onHistoryClick = viewModel::toggleConversationHistory,
            onNewChat = viewModel::newChat,
            onSettingsClick = onNavigateToSettings
        )
        // ── Model offline banner ─────────────────────────────────
        AnimatedVisibility(
            visible = !uiState.isModelAvailable,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),     
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,      
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "AI is offline",
                            style = MaterialTheme.typography.labelLarge,        
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer  
                        )
                        Text(
                            "Download the model in Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                    TextButton(onClick = onNavigateToSettings) {
                        Text("Settings")
                    }
                }
            }
        }

        // ── Messages or empty state ──────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.messages.isEmpty() && !uiState.isLoading) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedGemIcon(size = 72.dp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Ask me anything",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Powered by Gemma 4 — running entirely on your device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant      
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),     
                        verticalArrangement = Arrangement.spacedBy(8.dp)        
                    ) {
                        val suggestions = listOf(
                            "Set an alarm for 7:00 AM",
                            "Turn on the flashlight",
                            "Write a short poem about the moon",
                            "Explain quantum computing simply",
                            "What can you help me with?"
                        )
                        suggestions.forEach { suggestion ->
                            FeatureChip(
                                label = "\"$suggestion\"",
                                onClick = {
                                    viewModel.onInputChanged(suggestion)
                                    keyboardController?.hide()
                                    viewModel.sendMessage()
                                }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(uiState.messages, key = { it.id }) { message ->       
                        Column {
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium      
                                    )
                                ) + fadeIn()
                            ) {
                                ChatBubble(message = message, showInferenceStats = uiState.showInferenceStats)
                            }
                        }
                    }

                    // Streaming response with thinking — single bubble
                    if (uiState.thinkingText != null || uiState.streamingText != null) {
                        item(key = "streaming") {
                            val hasResponse = !uiState.streamingText.isNullOrEmpty()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(0.85f, fill = false)
                                        .animateContentSize(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ),
                                    shape = RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = 6.dp,
                                        bottomEnd = 20.dp
                                    ),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 1.dp
                                ) {
                                    Column {
                                        uiState.thinkingText?.let { thinking ->
                                            ThinkingSection(
                                                thinkingText = thinking,
                                                isStreaming = true,
                                                autoCollapse = hasResponse
                                            )
                                        }
                                        if (hasResponse) {
                                            Text(
                                                text = uiState.streamingText!!,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        uiState.streamingStats?.let { stats ->
                                            if (uiState.showInferenceStats) {
                                                InferenceStatsChip(stats)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Thinking indicator (dots) when waiting for first token
                    if (uiState.isLoading && uiState.streamingText == null && uiState.thinkingText == null) {   
                        item(key = "thinking") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                modifier = Modifier.padding(vertical = 4.dp)    
                            ) {
                                AnimatedGemIcon(size = 24.dp)
                                ThinkingIndicator()
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }

        // ── Input area ───────────────────────────────────────────
        Column(modifier = Modifier.navigationBarsPadding()) {
            // Pending image preview
            AnimatedVisibility(
                visible = uiState.pendingImageThumbnail != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                uiState.pendingImageThumbnail?.let { thumbBytes ->
                    PendingImagePreview(
                        thumbnailBytes = thumbBytes,
                        onRemove = viewModel::removeAttachment
                    )
                }
            }

            // Input bar with attachment popup overlay
            Box {
                ChatInputBar(
                    input = uiState.currentInput,
                    onInputChanged = viewModel::onInputChanged,
                    onSend = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        keyboardController?.hide()
                        viewModel.sendMessage()
                    },
                    onMicToggle = onMicToggle,
                    onStop = viewModel::stopGeneration,
                    onAttachmentClick = viewModel::toggleAttachmentOptions,
                    isRecording = uiState.isRecording,
                    isLoading = uiState.isLoading,
                    isEngineReady = uiState.isEngineReady,
                    hasAttachment = uiState.pendingImageBytes != null
                )

                // Attachment menu (Camera / Gallery / Tools)
                DropdownMenu(
                    expanded = uiState.showAttachmentOptions,
                    onDismissRequest = { viewModel.toggleAttachmentOptions() }
                ) {
                    DropdownMenuItem(
                        text = { Text("Camera", fontWeight = FontWeight.Medium) },
                        onClick = {
                            viewModel.toggleAttachmentOptions()
                            onNavigateToCapture()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Gallery", fontWeight = FontWeight.Medium) },
                        onClick = {
                            viewModel.toggleAttachmentOptions()
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Tools" + if (uiState.enabledTools.isNotEmpty()) " (${uiState.enabledTools.size} active)" else "",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = {
                            viewModel.toggleAttachmentOptions()
                            viewModel.toggleToolPicker()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }
        }
        }
    }

    // Tool picker bottom sheet
    if (uiState.showToolPicker) {
        ToolPickerBottomSheet(
            enabledTools = uiState.enabledTools,
            onToggleTool = { toolId, enabled ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.toggleTool(toolId, enabled)
            },
            onEnableAll = { viewModel.setAllToolsEnabled(true) },
            onDisableAll = { viewModel.setAllToolsEnabled(false) },
            onDismiss = viewModel::hideToolPicker
        )
    }

    // Conversation history bottom sheet
    if (uiState.showConversationHistory) {
        ConversationHistorySheet(
            conversations = uiState.conversations,
            currentConversationId = uiState.currentConversationId,
            onSelect = viewModel::loadConversation,
            onDelete = viewModel::deleteConversation,
            onDismiss = viewModel::hideConversationHistory
        )
    }
}





















// ── Pending Image Preview ────────────────────────────────────────────


// ── Tool Picker Bottom Sheet ─────────────────────────────────────────




// ── Conversation History Bottom Sheet ────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationHistorySheet(
    conversations: List<com.gemofgemma.core.data.ConversationEntity>,
    currentConversationId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    var pendingDelete by remember { mutableStateOf<com.gemofgemma.core.data.ConversationEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Conversations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (conversations.isEmpty()) {
                Text(
                    text = "No conversations yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(
                        count = conversations.size,
                        key = { conversations[it].id }
                    ) { index ->
                        val conv = conversations[index]
                        val isCurrent = conv.id == currentConversationId
                        val dateText = remember(conv.updatedAt) {
                            val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(conv.updatedAt))
                        }

                        ListItem(
                            headlineContent = {
                                Text(
                                    text = conv.title,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = dateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                if (!isCurrent) {
                                    IconButton(
                                        onClick = { pendingDelete = conv },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onSelect(conv.id) },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isCurrent)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else
                                    Color.Transparent
                            )
                        )
                    }
                }
            }
            pendingDelete?.let { conversation ->
                AlertDialog(
                    onDismissRequest = { pendingDelete = null },
                    title = { Text("Delete conversation?") },
                    text = { Text("Delete this conversation? This can't be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                pendingDelete = null
                                onDelete(conversation.id)
                            }
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}
