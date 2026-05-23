package com.pocketagent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pocketagent.app.ui.theme.AgentAccent
import com.pocketagent.app.ui.theme.AgentSuccess
import com.pocketagent.app.ui.theme.AgentTerminalGreen
import com.pocketagent.app.ui.theme.AgentWarning
import kotlinx.coroutines.launch

data class TaskStep(
    val message: String,
    val type: StepType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class StepType {
    PLANNING,   // Agent is thinking/planning
    EXECUTING,  // Agent is performing an action
    DONE,       // Step completed
    ERROR       // Step failed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    val steps = remember { mutableStateListOf<TaskStep>() }
    val listState = rememberLazyListState()

    // Auto-scroll to latest step
    LaunchedEffect(steps.size) {
        if (steps.isNotEmpty()) {
            listState.animateScrollToItem(steps.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务执行") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                isRunning = isRunning,
                onSend = {
                    if (inputText.isNotBlank()) {
                        scope.launch {
                            isRunning = true
                            executeTask(inputText, steps)
                            inputText = ""
                            isRunning = false
                        }
                    }
                },
                onStop = { isRunning = false }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (steps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "输入你想让 AI 帮你完成的任务\n\n例如：去抖音给周杰伦主页前3个视频点赞",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            items(steps) { step ->
                StepCard(step)
            }
        }
    }
}

@Composable
private fun StepCard(step: TaskStep) {
    val bgColor = when (step.type) {
        StepType.PLANNING -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        StepType.EXECUTING -> AgentAccent.copy(alpha = 0.15f)
        StepType.DONE -> AgentSuccess.copy(alpha = 0.1f)
        StepType.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
    }
    val iconColor = when (step.type) {
        StepType.PLANNING -> AgentWarning
        StepType.EXECUTING -> AgentAccent
        StepType.DONE -> AgentSuccess
        StepType.ERROR -> MaterialTheme.colorScheme.error
    }
    val label = when (step.type) {
        StepType.PLANNING -> "规划中"
        StepType.EXECUTING -> "执行中"
        StepType.DONE -> "完成"
        StepType.ERROR -> "失败"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "[$label]",
            style = MaterialTheme.typography.labelMedium,
            color = iconColor,
            modifier = Modifier.width(60.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = step.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BottomInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    isRunning: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "告诉 AI 你想做什么...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AgentAccent,
                    unfocusedBorderColor = AgentAccent.copy(alpha = 0.3f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isRunning) {
                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "停止",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = if (inputText.isNotBlank()) AgentAccent
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

/**
 * Stub task execution. In the real implementation, this sends the task
 * to the Python agent via the AgentBridge and receives real-time steps.
 */
private suspend fun executeTask(task: String, steps: MutableList<TaskStep>) {
    steps.add(TaskStep("解析任务: $task", StepType.PLANNING))
    kotlinx.coroutines.delay(1000)

    steps.add(TaskStep("连接到 agent 引擎...", StepType.EXECUTING))
    kotlinx.coroutines.delay(500)

    steps.add(TaskStep("规划操作步骤...", StepType.PLANNING))
    kotlinx.coroutines.delay(1500)

    steps.add(TaskStep("Agent 正在执行: 打开目标应用", StepType.EXECUTING))
    kotlinx.coroutines.delay(2000)
    steps.add(TaskStep("Agent 正在执行: 搜索目标内容", StepType.EXECUTING))
    kotlinx.coroutines.delay(2000)
    steps.add(TaskStep("Agent 正在执行: 执行操作...", StepType.EXECUTING))
    kotlinx.coroutines.delay(2000)

    steps.add(TaskStep("任务执行完毕，所有步骤已完成", StepType.DONE))
}