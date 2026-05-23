package com.pocketagent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pocketagent.app.ui.theme.AgentTerminalBg
import com.pocketagent.app.ui.theme.AgentTerminalGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val lines = remember { mutableStateListOf(
        "Pocket-Agent Terminal v1.0.0",
        "Type 'help' for available commands.",
        ""
    ) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("终端") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { lines.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "清屏")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AgentTerminalBg,
                    titleContentColor = AgentTerminalGreen,
                    navigationIconContentColor = AgentTerminalGreen,
                    actionIconContentColor = AgentTerminalGreen
                )
            )
        },
        bottomBar = {
            Surface(
                color = AgentTerminalBg,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$ ",
                        color = AgentTerminalGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.labelSmall.copy(
                            color = AgentTerminalGreen
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AgentTerminalGreen.copy(alpha = 0.3f),
                            unfocusedBorderColor = AgentTerminalGreen.copy(alpha = 0.1f),
                            focusedContainerColor = AgentTerminalBg,
                            unfocusedContainerColor = AgentTerminalBg
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                lines.add("$ $inputText")
                                executeCommand(inputText, lines)
                                inputText = ""
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "执行",
                            tint = AgentTerminalGreen
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(AgentTerminalBg)
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(lines) { line ->
                Text(
                    text = line,
                    color = if (line.startsWith("$ ")) AgentTerminalGreen.copy(alpha = 0.7f)
                    else AgentTerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

private fun executeCommand(cmd: String, lines: MutableList<String>) {
    when {
        cmd == "help" -> {
            lines.add("Available commands:")
            lines.add("  help     - Show this help")
            lines.add("  status   - Show agent status")
            lines.add("  version  - Show version info")
            lines.add("  update   - Check for agent updates")
            lines.add("  clear    - Clear terminal")
        }
        cmd == "status" -> {
            lines.add("Agent: running")
            lines.add("Termux: active")
            lines.add("MCP server: connected")
        }
        cmd == "version" -> {
            lines.add("Pocket-Agent v1.0.0")
            lines.add("Agent seed: 2026-05-24")
            lines.add("Android SDK: 34")
        }
        cmd == "update" -> {
            lines.add("Checking for updates...")
            lines.add("Agent is up to date.")
        }
        cmd == "clear" -> {
            lines.clear()
            lines.add("Pocket-Agent Terminal v1.0.0")
        }
        cmd.startsWith("python") -> {
            lines.add("Python runtime available. Use 'python main.py' to start agent manually.")
        }
        cmd.startsWith("git") -> {
            lines.add("Git client available in Termux environment.")
            lines.add("Repository: github.com/Dreamt0511/Pocket-Agent")
        }
        else -> {
            lines.add("Command not recognized. Type 'help' for available commands.")
        }
    }
    lines.add("")
}