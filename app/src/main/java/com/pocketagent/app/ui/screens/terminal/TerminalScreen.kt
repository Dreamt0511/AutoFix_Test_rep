package com.pocketagent.app.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketagent.app.bridge.AgentDaemon
import com.pocketagent.app.ui.theme.PocketAgentTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen() {
    val daemonState by AgentDaemon.daemonState.collectAsState()
    val streamOutput by AgentDaemon.streamOutput.collectAsState()
    val diagnostics by AgentDaemon.diagnostics.collectAsState()

    var command by remember { mutableStateOf("") }
    var showDiagnostics by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val terminalLines = remember { mutableStateListOf<TerminalLine>() }

    // 监听流式输出并追加到终端行
    LaunchedEffect(streamOutput) {
        if (streamOutput.isNotBlank()) {
            val newLines = streamOutput.lines().filter { it.isNotBlank() }
            terminalLines.addAll(newLines.map { TerminalLine(it, TerminalLineType.OUTPUT) })
        }
    }

    PocketAgentTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("终端")
                            Spacer(modifier = Modifier.width(8.dp))
                            // 状态指示灯
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = when (daemonState) {
                                            AgentDaemon.DaemonState.RUNNING -> Color(0xFF4CAF50)
                                            AgentDaemon.DaemonState.STARTING -> Color(0xFFFFC107)
                                            AgentDaemon.DaemonState.ERROR -> Color(0xFFF44336)
                                            else -> Color(0xFF9E9E9E)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    },
                    actions = {
                        // 诊断按钮
                        IconButton(onClick = { showDiagnostics = !showDiagnostics }) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "诊断",
                                tint = if (showDiagnostics) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                            )
                        }

                        // 清屏
                        IconButton(onClick = { terminalLines.clear() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "清屏"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                // 输入栏
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$ ",
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        BasicTextField(
                            value = command,
                            onValueChange = { command = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = false,
                            maxLines = 3,
                            decorationBox = { innerTextField ->
                                if (command.isEmpty()) {
                                    Text(
                                        text = "输入指令...",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (command.isNotBlank()) {
                                    terminalLines.add(TerminalLine("$ $command", TerminalLineType.COMMAND))
                                    command = ""
                                }
                            },
                            enabled = command.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "发送",
                                tint = if (command.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 诊断面板（可折叠）
                if (showDiagnostics) {
                    DiagnosticPanel(diagnostics)
                    HorizontalDivider()
                }

                // 终端输出区
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    state = listState,
                ) {
                    if (terminalLines.isEmpty()) {
                        item {
                            Text(
                                text = "Pocket Agent 终端模式\nready...\n",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    items(terminalLines) { line ->
                        TerminalLineView(line)
                    }
                }
            }
        }
    }
}

enum class TerminalLineType { COMMAND, OUTPUT, ERROR, SYSTEM }

data class TerminalLine(
    val text: String,
    val type: TerminalLineType
)

@Composable
fun TerminalLineView(line: TerminalLine) {
    val color = when (line.type) {
        TerminalLineType.COMMAND -> Color(0xFF00E5FF)
        TerminalLineType.OUTPUT -> Color(0xFFE0E0E0)
        TerminalLineType.ERROR -> Color(0xFFFF6B6B)
        TerminalLineType.SYSTEM -> Color(0xFF81C784)
    }

    Text(
        text = line.text,
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    )
}

@Composable
fun DiagnosticPanel(report: AgentDaemon.DiagnosticReport) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "系统诊断",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            DiagnosticRow("Python 版本", report.pythonVersion)
            DiagnosticRow(
                "依赖包",
                "${report.pipPackages.size} 个已安装",
                isOk = report.pipPackages.isNotEmpty()
            )
            DiagnosticRow(
                "配置文件",
                if (report.configValid) "有效" else "无效",
                isOk = report.configValid
            )
            DiagnosticRow(
                "API 连接",
                if (report.apiReachable) "可到达" else "不可到达",
                isOk = report.apiReachable
            )

            if (report.errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "错误: ${report.errorMessage}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String, isOk: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isOk) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}