package com.pocketagent.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketagent.app.data.SettingsRepository
import com.pocketagent.app.ui.theme.AgentAccent
import com.pocketagent.app.ui.theme.AgentTerminalGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTask: () -> Unit,
    onNavigateToTerminal: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val hasApiKey by settingsRepo.hasApiKey.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pocket-Agent") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = onNavigateToTerminal) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = "Terminal",
                            tint = AgentTerminalGreen
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!hasApiKey) {
                SetupPrompt(onNavigateToTask = onNavigateToTask)
            } else {
                ReadyState(onNavigateToTask = onNavigateToTask)
            }
        }
    }
}

@Composable
private fun SetupPrompt(onNavigateToTask: () -> Unit) {
    Text(
        text = "POCKET-AGENT",
        style = MaterialTheme.typography.headlineLarge,
        color = AgentAccent
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "你的手机 AI 操作员",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
    Spacer(modifier = Modifier.height(32.dp))
    Text(
        text = "配置 API key 后，用自然语言让 AI 帮你操控手机",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(32.dp))

    // Temporary: go directly to task screen (setup will intercept if needed)
    Button(
        onClick = onNavigateToTask,
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Icon(Icons.Default.Settings, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("开始配置")
    }
}

@Composable
private fun ReadyState(onNavigateToTask: () -> Unit) {
    Text(
        text = "POCKET-AGENT",
        style = MaterialTheme.typography.headlineLarge,
        color = AgentAccent
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "就绪",
        style = MaterialTheme.typography.titleMedium,
        color = AgentTerminalGreen
    )
    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = "",
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text("告诉 AI 你想做什么...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        },
        enabled = false,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AgentAccent,
            unfocusedBorderColor = AgentAccent.copy(alpha = 0.3f)
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onNavigateToTask,
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("开始任务")
    }
}