package com.gemofgemma.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gemofgemma.core.model.ToolCategory
import com.gemofgemma.core.model.ToolDefinition
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ToolPickerBottomSheet(
    enabledTools: Set<String>,
    onToggleTool: (String, Boolean) -> Unit,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tools",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEnableAll) {
                    Text("Enable All")
                }
                TextButton(onClick = onDisableAll) {
                    Text("Disable All")
                }
            }

            Text(
                text = "${enabledTools.size} of ${ToolDefinition.ALL_TOOLS.size} active",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Search / filter field
            var filterQuery by remember { mutableStateOf("") }
            TextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        "Filter tools\u2026",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear filter",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            // Group and filter tools by category
            val query = filterQuery.trim()
            val grouped = ToolDefinition.ALL_TOOLS
                .filter { tool ->
                    query.isEmpty() ||
                        tool.name.contains(query, ignoreCase = true) ||
                        tool.description.contains(query, ignoreCase = true)
                }
                .groupBy { it.category }
            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                ToolCategory.entries.forEach { category ->
                    val toolsInCategory = grouped[category] ?: return@forEach
                    item(key = "header_${category.name}") {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(
                        count = toolsInCategory.size,
                        key = { toolsInCategory[it].id }
                    ) { index ->
                        val tool = toolsInCategory[index]
                        val isEnabled = enabledTools.contains(tool.id)
                        ToolPickerItem(
                            tool = tool,
                            isEnabled = isEnabled,
                            onToggle = { checked -> onToggleTool(tool.id, checked) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ToolPickerItem(
    tool: ToolDefinition,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val perm = tool.requiredPermission
    if (perm != null) {
        val permissionState = rememberPermissionState(perm)
        var triggerToggled by remember { mutableStateOf(false) }

        LaunchedEffect(permissionState.status.isGranted, triggerToggled) {
            if (triggerToggled && permissionState.status.isGranted && !isEnabled) {
                onToggle(true)
                triggerToggled = false
            } else if (!permissionState.status.isGranted && isEnabled) {
                onToggle(false)
            }
        }

        ListItem(
            headlineContent = { Text(tool.name, fontWeight = FontWeight.Medium) },
            supportingContent = { Text(tool.description, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            trailingContent = {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (permissionState.status.isGranted) {
                                onToggle(true)
                            } else {
                                triggerToggled = true
                                permissionState.launchPermissionRequest()
                            }
                        } else {
                            onToggle(false)
                        }
                    }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    } else {
        ListItem(
            headlineContent = { Text(tool.name, fontWeight = FontWeight.Medium) },
            supportingContent = { Text(tool.description, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            trailingContent = {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked -> onToggle(checked) }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
