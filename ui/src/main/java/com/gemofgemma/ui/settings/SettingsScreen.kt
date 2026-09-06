package com.gemofgemma.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemofgemma.core.AiModelInfo
import com.gemofgemma.core.model.ToolDefinition
import com.gemofgemma.ui.model.ModelStatusViewModel
import com.gemofgemma.ui.model.formatSize
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@Composable
fun SettingsScreen(
    viewModel: ModelStatusViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val enabledTools by settingsViewModel.enabledTools.collectAsStateWithLifecycle()
    val showInferenceStats by settingsViewModel.showInferenceStats.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val activeModelId by viewModel.activeModelId.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadedBytes by viewModel.downloadedBytes.collectAsStateWithLifecycle()
    val totalBytes by viewModel.totalBytes.collectAsStateWithLifecycle()
    val downloadError by viewModel.downloadError.collectAsStateWithLifecycle()
    val toolsByCategory = remember { ToolDefinition.ALL_TOOLS.groupBy { it.category } }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Spacer(Modifier.height(16.dp)) }
        item { SectionHeader("Models", Modifier.padding(bottom = 8.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                viewModel.models.forEach { model ->
                    ModelCard(
                        model = model,
                        isActive = model.id == activeModelId,
                        isInstalled = viewModel.isInstalled(model.id),
                        isDownloading = isDownloading && model.id == activeModelId,
                        progress = if (model.id == activeModelId) downloadProgress else 0f,
                        downloadedBytes = if (model.id == activeModelId) downloadedBytes else viewModel.sizeOnDisk(model.id),
                        totalBytes = if (model.id == activeModelId) totalBytes else model.expectedSizeBytes,
                        hasPartialDownload = viewModel.hasPartialDownload(model.id),
                        onDownload = { viewModel.downloadModel(model.id) },
                        onDelete = { viewModel.deleteModel(model.id) },
                        onSelect = { viewModel.selectModel(model.id) }
                    )
                }
            }
        }
        if (downloadError != null) {
            item { Text(downloadError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp)) }
        }
        item { Spacer(Modifier.height(24.dp)) }

        toolsByCategory.forEach { (category, tools) ->
            item { SectionHeader(category.title, Modifier.padding(bottom = 8.dp)) }
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column {
                        tools.forEachIndexed { index, tool ->
                            ToolListItem(tool, enabledTools.contains(tool.id)) { enabled ->
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                settingsViewModel.toggleTool(tool, enabled)
                            }
                            if (index < tools.size - 1) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        item { SectionHeader("Appearance", Modifier.padding(bottom = 8.dp)) }
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Theme") },
                        supportingContent = { Text("Follows your Android system light/dark setting") },
                        leadingContent = { Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Show inference stats") },
                        supportingContent = { Text("Display tokens/sec, TTFT, and backend below each response") },
                        leadingContent = { Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = {
                            Switch(
                                checked = showInferenceStats,
                                onCheckedChange = settingsViewModel::setShowInferenceStats
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: AiModelInfo,
    isActive: Boolean,
    isInstalled: Boolean,
    isDownloading: Boolean,
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    hasPartialDownload: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (model.recommended) Icons.Default.AutoAwesome else Icons.Default.Memory, null, tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(model.name, fontWeight = FontWeight.SemiBold)
                        if (model.recommended) {
                            Spacer(Modifier.width(8.dp))
                            AssistChip(onClick = {}, label = { Text("Recommended") }, enabled = false)
                        }
                    }
                    Text(model.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isActive) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, enabled = false, label = { Text("${model.minRamGb} GB+ RAM") })
                AssistChip(onClick = {}, enabled = false, label = { Text(if (model.supportsVision) "Vision" else "Text") })
                AssistChip(onClick = {}, enabled = false, label = { Text(formatSize(model.expectedSizeBytes)) })
            }
            if (isDownloading) {
                Spacer(Modifier.height(12.dp))
                Text("Downloading ${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)))
                Spacer(Modifier.height(4.dp))
                Text("${formatSize(downloadedBytes)} / ${formatSize(totalBytes)}", style = MaterialTheme.typography.bodySmall)
            } else {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isInstalled && !isActive) {
                        Button(onClick = onSelect) { Text("Use model") }
                    } else if (!isInstalled) {
                        Button(onClick = onDownload) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text(if (hasPartialDownload) "Resume" else "Download") }
                    } else {
                        OutlinedButton(onClick = {}, enabled = false) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(6.dp)); Text("Active") }
                    }
                    if (isInstalled) {
                        TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ToolListItem(tool: ToolDefinition, isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    val perm = tool.requiredPermission
    if (perm != null) {
        val permissionState = rememberPermissionState(perm)
        var triggerToggled by remember { mutableStateOf(false) }
        LaunchedEffect(permissionState.status.isGranted, triggerToggled) {
            if (triggerToggled && permissionState.status.isGranted && !isEnabled) { onToggle(true); triggerToggled = false }
            else if (!permissionState.status.isGranted && isEnabled) onToggle(false)
        }
        ListItem(
            headlineContent = { Text(tool.name, fontWeight = FontWeight.Medium) },
            supportingContent = { Text(tool.description) },
            leadingContent = { Icon(if (tool.isDangerousPermission) Icons.Default.Warning else Icons.Default.Settings, null, tint = if (tool.isDangerousPermission) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingContent = {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (permissionState.status.isGranted) onToggle(true)
                            else { triggerToggled = true; permissionState.launchPermissionRequest() }
                        } else onToggle(false)
                    }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    } else {
        ListItem(
            headlineContent = { Text(tool.name, fontWeight = FontWeight.Medium) },
            supportingContent = { Text(tool.description) },
            leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingContent = {
                Switch(checked = isEnabled, onCheckedChange = onToggle)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = modifier.padding(start = 8.dp))
}
