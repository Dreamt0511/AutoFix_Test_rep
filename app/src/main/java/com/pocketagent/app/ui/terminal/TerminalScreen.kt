package com.pocketagent.app.ui.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

/**
 * 终端页面 - Termux 集成
 * 
 * 功能:
 * 1. Termux 命令执行
 * 2. 环境传感器读取 (光感、加速度)
 * 3. 系统信息查询
 * 4. 命令历史记录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }
    var terminalLines by remember { mutableStateOf<List<TerminalLine>>(emptyList()) }
    var currentDir by remember { mutableStateOf("~") }

    // 初始化终端
    LaunchedEffect(Unit) {
        terminalLines = terminalLines + TerminalLine(
            text = "Termux 终端就绪",
            type = TerminalLineType.INFO,
            timestamp = System.currentTimeMillis()
        )
        terminalLines = terminalLines + TerminalLine(
            text = "输入 'help' 查看可用命令",
            type = TerminalLineType.INFO,
            timestamp = System.currentTimeMillis()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("终端") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
                    }
                },
                actions = {
                    // 环境状态指示器
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // TODO: 显示电池、网络等状态
                    }
                }
            )
        },
        bottomBar = {
            TerminalInputBar(
                currentDir = currentDir,
                inputText = inputText,
                onInputChange = { inputText = it },
                onExecute = {
                    if (inputText.isNotBlank() && !isExecuting) {
                        scope.launch {
                            isExecuting = true
                            
                            // 显示命令
                            terminalLines = terminalLines + TerminalLine(
                                text = "$currentDir $ ${inputText}",
                                type = TerminalLineType.COMMAND,
                                timestamp = System.currentTimeMillis()
                            )
                            
                            // 执行命令
                            val result = executeTermuxCommand(inputText)
                            
                            // 显示结果
                            terminalLines = terminalLines + TerminalLine(
                                text = result,
                                type = TerminalLineType.OUTPUT,
                                timestamp = System.currentTimeMillis()
                            )
                            
                            inputText = ""
                            isExecuting = false
                            
                            // 更新当前目录
                            if (inputText.startsWith("cd ")) {
                                currentDir = updateCurrentDir(inputText, currentDir)
                            }
                            
                            // 滚动到底部
                            listState.animateScrollToItem(terminalLines.size - 1)
                        }
                    }
                },
                enabled = !isExecuting
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = listState,
            reverseLayout = false,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(terminalLines) { line ->
                TerminalLineItem(line)
            }
        }
    }
}

// ─── 终端行模型 ─────────────────────────────────

data class TerminalLine(
    val text: String,
    val type: TerminalLineType,
    val timestamp: Long
)

enum class TerminalLineType {
    COMMAND, OUTPUT, ERROR, INFO, WARNING
}

// ─── 终端行显示 ─────────────────────────────────

@Composable
private fun TerminalLineItem(line: TerminalLine) {
    val color = when (line.type) {
        TerminalLineType.COMMAND -> Color(0xFF4CAF50)  // 绿色
        TerminalLineType.OUTPUT -> Color(0xFFE0E0E0)   // 浅灰
        TerminalLineType.ERROR -> Color(0xFFF44336)    // 红色
        TerminalLineType.INFO -> Color(0xFF2196F3)     // 蓝色
        TerminalLineType.WARNING -> Color(0xFFFF9800)  // 橙色
    }

    Text(
        text = line.text,
        color = color,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
    )
}

// ─── 输入栏 ────────────────────────────────────

@Composable
private fun TerminalInputBar(
    currentDir: String,
    inputText: String,
    onInputChange: (String) -> Unit,
    onExecute: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 当前路径提示
            Text(
                text = currentDir,
                color = Color(0xFF4CAF50),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 提示符
                Text(
                    text = "$",
                    color = Color(0xFF4CAF50),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                // 输入框
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入 Termux 命令...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFF666666)
                    ),
                    enabled = enabled
                )

                // 执行按钮
                Button(
                    onClick = onExecute,
                    enabled = inputText.isNotBlank() && enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("执行")
                }
            }

            // 快捷命令按钮
            QuickCommandsBar(
                onCommandClick = { cmd ->
                    onInputChange(cmd)
                }
            )
        }
    }
}

// ─── 快捷命令 ───────────────────────────────────

@Composable
private fun QuickCommandsBar(
    onCommandClick: (String) -> Unit
) {
    val quickCommands = listOf(
        "pwd" to "当前路径",
        "ls" to "列出文件",
        "termux-battery-status" to "电池状态",
        "termux-sensor" to "传感器",
        "termux-wifi-scaninfo" to "WiFi扫描",
        "help" to "帮助"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        quickCommands.forEach { (cmd, label) ->
            OutlinedButton(
                onClick = { onCommandClick(cmd) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// ─── Termux 命令执行 (模拟) ─────────────────────

private fun executeTermuxCommand(command: String): String {
    return when {
        command == "help" -> """
            可用命令:
            • pwd - 显示当前目录
            • ls - 列出文件
            • cd [dir] - 切换目录
            • termux-battery-status - 电池状态
            • termux-sensor - 传感器数据
            • termux-wifi-scaninfo - WiFi扫描
            • termux-notification - 发送通知
            • termux-vibrate - 振动
            • termux-tts-speak - 语音合成
            • exit - 退出终端
        """.trimIndent()
        
        command == "pwd" -> "/data/data/com.termux/files/home"
        command.startsWith("cd ") -> "目录已切换"
        command == "ls" -> """
            Android  README.md  storage  usr
            bin      home       tmp      var
        """.trimIndent()
        
        command == "termux-battery-status" -> """
            {
                "health": "GOOD",
                "percentage": 85,
                "plugged": "UNPLUGGED",
                "status": "DISCHARGING",
                "temperature": 30.5
            }
        """.trimIndent()
        
        command == "termux-sensor" -> """
            可用传感器:
            • Accelerometer - 加速度计
            • Ambient Light - 环境光
            • Proximity - 距离传感器
            • Gyroscope - 陀螺仪
        """.trimIndent()
        
        command == "exit" -> "退出终端"
        else -> "命令未找到: $command"
    }
}

private fun updateCurrentDir(command: String, currentDir: String): String {
    val dir = command.removePrefix("cd ").trim()
    return when {
        dir == "~" -> "~"
        dir == ".." -> ".."
        dir.startsWith("/") -> dir
        else -> "$currentDir/$dir"
    }
}