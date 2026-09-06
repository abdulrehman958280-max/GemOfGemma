package com.gemofgemma.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ChatTopBar(onHistoryClick: () -> Unit, onNewChat: () -> Unit, onSettingsClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), tonalElevation = 2.dp) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SmartToy, contentDescription = "OmniCode", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("OmniCode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onHistoryClick, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Forum, contentDescription = "Chat History", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onNewChat, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Add, contentDescription = "New Chat", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
