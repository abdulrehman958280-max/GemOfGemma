package com.gemofgemma.ui.chat

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun ChatInputBar(
    input: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicToggle: () -> Unit,
    onStop: () -> Unit,
    onAttachmentClick: () -> Unit,
    isRecording: Boolean,
    isLoading: Boolean,
    isEngineReady: Boolean = true,
    hasAttachment: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hasText = input.isNotBlank()
    val canSend = (hasText || hasAttachment) && !isLoading && isEngineReady

    // Three-state button: STOP (2) when loading, SEND (1) when text, MIC (0) otherwise
    val buttonState = when {
        isLoading -> 2
        hasText -> 1
        else -> 0
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Engine loading banner
        AnimatedVisibility(visible = !isEngineReady) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer  
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI model is loading…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer  
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Single "+" attach button
                IconButton(
                    onClick = onAttachmentClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (hasAttachment)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach",
                        tint = if (hasAttachment)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pill-shaped text field with mic/send inside
                TextField(
                    value = input,
                    onValueChange = onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (isRecording) "Listening…"
                                else if (!isEngineReady) "AI loading…"
                                else "Message…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        Crossfade(
                            targetState = buttonState,
                            animationSpec = tween(200),
                            label = "input_action"
                        ) { state ->
                            when (state) {
                                2 -> {
                                    IconButton(
                                        onClick = onStop,
                                        modifier = Modifier.size(48.dp),
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Stop,
                                            contentDescription = "Stop generating",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                1 -> {
                                    IconButton(
                                        onClick = onSend,
                                        enabled = canSend,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = if (canSend)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                else -> {
                                    IconButton(
                                        onClick = onMicToggle,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                                            contentDescription = if (isRecording) "Stop recording" else "Voice input",
                                            tint = if (isRecording)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun PendingImagePreview(
    thumbnailBytes: ByteArray,
    onRemove: () -> Unit
) {
    val bitmap = remember(thumbnailBytes) {
        BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.size)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(modifier = Modifier.size(60.dp)) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Pending image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                // Remove button
                Surface(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.padding(3.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}
