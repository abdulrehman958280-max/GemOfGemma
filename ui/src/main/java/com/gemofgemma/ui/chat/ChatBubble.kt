package com.gemofgemma.ui.chat

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gemofgemma.core.model.ChatMessage
import com.gemofgemma.ui.camera.BoundingBoxOverlay
import com.gemofgemma.ui.components.ThinkingIndicator

@Composable
fun ChatBubble(message: ChatMessage, showInferenceStats: Boolean = true) {
    val isUser = message.role == ChatMessage.Role.USER
    val hasImage = message.imageBytes != null
    val context = LocalContext.current
    var showCopyButton by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
        Surface(
            modifier = Modifier
                .weight(0.85f, fill = false)
                .animateContentSize()
                .then(
                    if (!isUser) Modifier.clickable { showCopyButton = !showCopyButton }
                    else Modifier
                ),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp
            ),
            color = when {
                message.messageType == ChatMessage.MessageType.ERROR ->
                    MaterialTheme.colorScheme.errorContainer
                isUser -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
            tonalElevation = if (isUser) 0.dp else 1.dp
        ) {
            Column {
                // Show collapsible thinking section for finalized messages
                val thinking = message.thinkingContent
                if (!isUser && thinking != null) {
                    ThinkingSection(
                        thinkingText = thinking,
                        isStreaming = false
                    )
                }

                when (message.messageType) {
                    ChatMessage.MessageType.IMAGE_QUERY -> ImageQueryContent(message, isUser)
                    ChatMessage.MessageType.DETECTION -> DetectionContent(message)
                    ChatMessage.MessageType.OCR -> OcrContent(message)
                    ChatMessage.MessageType.ACTION -> ActionContent(message)
                    ChatMessage.MessageType.ERROR -> ErrorContent(message)
                    else -> TextContent(message, isUser)
                }

                // Inference stats chip — assistant messages only
                if (!isUser && message.stats != null && showInferenceStats) {
                    InferenceStatsChip(message.stats!!)
                }
            }
        }
        }

        // Copy button for assistant messages — shown on tap
        AnimatedVisibility(
            visible = !isUser && message.content.isNotBlank() && showCopyButton
        ) {
            var copied by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("OmniCode", message.content))
                        copied = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = if (copied) "Copied" else "Copy response",
                        modifier = Modifier.size(14.dp),
                        tint = if (copied) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                if (copied) {
                    Text(
                        text = "Copied",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        copied = false
                    }
                }
            }
        }
    }
}

@Composable
fun InferenceStatsChip(stats: com.gemofgemma.core.model.InferenceStats) {
    Text(
        text = stats.formatDisplay(),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
}

@Composable
fun TextContent(message: ChatMessage, isUser: Boolean) {
    Text(
        text = message.content,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isUser)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun ImageQueryContent(message: ChatMessage, isUser: Boolean) {
    val bitmap = remember(message.id) {
        message.imageBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Attached image",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                ),
            contentScale = ContentScale.Crop
        )
    }
    if (message.content.isNotBlank()) {
        Text(
            text = message.content,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUser)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DetectionContent(message: ChatMessage) {
    val bitmap = remember(message.id) {
        message.imageBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
    val detections = message.detections.orEmpty()
    if (bitmap != null && detections.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Detected objects",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
            BoundingBoxOverlay(
                detections = detections,
                imageWidth = 1000,
                imageHeight = 1000,
                modifier = Modifier.matchParentSize()
            )
        }
    }

    // Detection summary
    val summary = if (detections.isNotEmpty()) {
        val grouped = detections.groupBy { it.label.lowercase() }
        val parts = grouped.map { (label, items) ->
            if (items.size > 1) "${items.size} ${label}s" else label
        }
        "Found ${detections.size} object${if (detections.size != 1) "s" else ""}: ${parts.joinToString(", ")}"
    } else {
        message.content
    }

    Text(
        text = summary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun OcrContent(message: ChatMessage) {
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = "Extracted Text",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            SelectionContainer {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            onClick = {
                clipboardManager.setText(AnnotatedString(message.content))
            },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.align(Alignment.End)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun ActionContent(message: ChatMessage) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ErrorContent(message: ChatMessage) {
    Text(
        text = message.content,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer
    )
}

@Composable
fun ThinkingSection(
    thinkingText: String,
    isStreaming: Boolean,
    autoCollapse: Boolean = false,
    modifier: Modifier = Modifier
) {
    // During streaming: expanded until response starts, then auto-collapse.
    // After streaming: default collapsed.
    var expanded by remember { mutableStateOf(isStreaming) }

    // Auto-collapse when response text starts appearing
    LaunchedEffect(autoCollapse) {
        if (autoCollapse) expanded = false
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
        ) {
            // Header row — always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\uD83D\uDCAD",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = if (isStreaming && !autoCollapse) "Thinking\u2026" else "Thought process",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isStreaming && !autoCollapse) {
                    ThinkingIndicator(
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (expanded) "\u25BE" else "\u25B8",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Collapsible content
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = thinkingText,
                    modifier = Modifier.padding(
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 8.dp
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    maxLines = if (isStreaming) Int.MAX_VALUE else 50,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
