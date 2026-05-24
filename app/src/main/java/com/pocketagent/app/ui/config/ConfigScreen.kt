package com.pocketagent.app.ui.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketagent.app.core.AppBootstrapper
import kotlinx.coroutines.launch

/**
 * 配置管理页面 - 基于主项目的 .env.example 和 config.py
 * 
 * 支持:
 * 1. 主 Agent 模型配置 (LLM_BASE_URL, API_KEY, MODEL, TEMPERATURE, MAX_TOKENS)
 * 2. 子 Agent 模型配置 (EXECUTOR_LLM_BASE_URL, EXECUTOR_API_KEY, EXECUTOR_MODEL...)
 * 3. MCP 服务器配置
 * 4. 本地/云端环境切换
 * 5. Termux API 配置
 */
@Composable
fun ConfigScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var configs by remember { mutableStateOf<List<ConfigItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var saveSuccess by remember { mutableStateOf(false) }

    // 加载配置
    LaunchedEffect(Unit) {
        ConfigManager.init(android.content.ContextWrapper(navController.context))
        configs = loadConfigs()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置管理") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
                    }
                },
                actions = {
                    if (saveSuccess) {
                        Text(
                            "已保存",
                            color = Color.Green,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 配置组标题
                configs.groupBy { it.group }.forEach { (group, items) ->
                    item {
                        ConfigGroupHeader(group)
                    }
                    items(items) { item ->
                        ConfigItemCard(item) { updated ->
                            scope.launch {
                                saveConfig(updated)
                                saveSuccess = true
                                // 5秒后隐藏保存提示
                                kotlinx.coroutines.delay(5000)
                                saveSuccess = false
                            }
                        }
                    }
                }

                // 操作按钮
                item {
                    ConfigActions(
                        onSave = {
                            scope.launch {
                                saveAllConfigs(configs)
                                saveSuccess = true
                                kotlinx.coroutines.delay(5000)
                                saveSuccess = false
                            }
                        },
                        onReset = {
                            configs = loadDefaultConfigs()
                        },
                        onSync = {
                            scope.launch {
                                AppBootstrapper.forceSync()
                            }
                        }
                    )
                }
            }
        }
    }
}

// ─── 配置项定义 ─────────────────────────────────

data class ConfigItem(
    val key: String,
    var value: String,
    val description: String,
    val group: String,
    val type: ConfigType,
    val required: Boolean = false,
    val isSecret: Boolean = false,
    val options: List<String>? = null
)

enum class ConfigType {
    TEXT, SECRET, NUMBER, BOOLEAN, DROPDOWN, URL
}

// ─── 配置加载逻辑 ───────────────────────────────

private fun loadConfigs(): List<ConfigItem> {
    return listOf(
        // ==================== MCP 服务器配置 ====================
        ConfigItem(
            key = "MCP_SERVER_URL",
            value = "http://127.0.0.1:7474/mcp",
            description = "MCP 服务器地址",
            group = "MCP 服务器",
            type = ConfigType.URL,
            required = true
        ),

        // ==================== 主 Agent 模型配置 (云端) ====================
        ConfigItem(
            key = "DEFAULT_LLM_BASE_URL",
            value = "https://api.longcat.chat/openai/v1",
            description = "主 Agent LLM 服务地址 (OpenAI 兼容)",
            group = "主 Agent 模型配置 (云端)",
            type = ConfigType.URL
        ),
        ConfigItem(
            key = "LLM_API_KEY",
            value = "",
            description = "主 Agent API 密钥",
            group = "主 Agent 模型配置 (云端)",
            type = ConfigType.SECRET,
            isSecret = true
        ),
        ConfigItem(
            key = "LLM_MODEL",
            value = "longcat-flash-lite",
            description = "主 Agent 模型名称",
            group = "主 Agent 模型配置 (云端)",
            type = ConfigType.TEXT
        ),
        ConfigItem(
            key = "LLM_TEMPERATURE",
            value = "0.7",
            description = "主 Agent 温度参数 (0.0-2.0)",
            group = "主 Agent 模型配置 (云端)",
            type = ConfigType.NUMBER
        ),
        ConfigItem(
            key = "LLM_MAX_TOKENS",
            value = "8000",
            description = "主 Agent 最大输出 token 数",
            group = "主 Agent 模型配置 (云端)",
            type = ConfigType.NUMBER
        ),

        // ==================== 主 Agent 模型配置 (本地) ====================
        ConfigItem(
            key = "DEFAULT_LLM_BASE_URL_LOCAL",
            value = "http://127.0.0.1:8080/v1",
            description = "手机端本地 llama-server 地址",
            group = "主 Agent 模型配置 (本地)",
            type = ConfigType.URL
        ),
        ConfigItem(
            key = "LLM_API_KEY_LOCAL",
            value = "dummy",
            description = "本地 API 密钥 (通常为 dummy)",
            group = "主 Agent 模型配置 (本地)",
            type = ConfigType.TEXT
        ),
        ConfigItem(
            key = "LLM_MODEL_LOCAL",
            value = "gelab-zero-4b-preview",
            description = "本地模型名称",
            group = "主 Agent 模型配置 (本地)",
            type = ConfigType.TEXT
        ),

        // ==================== 子 Agent (Executor) 模型配置 ====================
        ConfigItem(
            key = "EXECUTOR_LLM_BASE_URL",
            value = "",
            description = "子 Agent LLM 服务地址 (留空则使用主 Agent 配置)",
            group = "子 Agent 模型配置",
            type = ConfigType.URL
        ),
        ConfigItem(
            key = "EXECUTOR_API_KEY",
            value = "",
            description = "子 Agent API 密钥 (留空则使用主 Agent 配置)",
            group = "子 Agent 模型配置",
            type = ConfigType.SECRET,
            isSecret = true
        ),
        ConfigItem(
            key = "EXECUTOR_MODEL",
            value = "qwen2.5:7b",
            description = "子 Agent 模型名称",
            group = "子 Agent 模型配置",
            type = ConfigType.TEXT
        ),
        ConfigItem(
            key = "EXECUTOR_TEMPERATURE",
            value = "0.3",
            description = "子 Agent 温度参数 (0.0-2.0)",
            group = "子 Agent 模型配置",
            type = ConfigType.NUMBER
        ),
        ConfigItem(
            key = "EXECUTOR_MAX_TOKENS",
            value = "8192",
            description = "子 Agent 最大输出 token 数",
            group = "子 Agent 模型配置",
            type = ConfigType.NUMBER
        ),

        // ==================== Ollama 配置 ====================
        ConfigItem(
            key = "OLLAMA_BASE_URL",
            value = "http://localhost:11434",
            description = "Ollama 本地服务地址 (可选)",
            group = "Ollama 配置",
            type = ConfigType.URL
        ),

        // ==================== Termux API 配置 ====================
        ConfigItem(
            key = "TERMUX_API_ENABLED",
            value = "true",
            description = "启用 Termux API 功能",
            group = "Termux API 配置",
            type = ConfigType.BOOLEAN
        ),
        ConfigItem(
            key = "TERMUX_API_CHECK_CMD",
            value = "which termux-battery-status",
            description = "Termux API 检测命令",
            group = "Termux API 配置",
            type = ConfigType.TEXT
        ),

        // ==================== 环境感知配置 ====================
        ConfigItem(
            key = "ENV_LIGHT_SENSOR_CMD",
            value = "termux-sensor -s \"tcs3760 Ambient Light Sensor Non-wakeup\" -n 1",
            description = "环境光传感器命令",
            group = "环境感知配置",
            type = ConfigType.TEXT
        ),
        ConfigItem(
            key = "ENV_ACCEL_SENSOR_CMD",
            value = "termux-sensor -s \"lsm6dsv Accelerometer Non-wakeup\" -n 3",
            description = "加速度传感器命令",
            group = "环境感知配置",
            type = ConfigType.TEXT
        )
    )
}

private fun loadDefaultConfigs(): List<ConfigItem> {
    // 返回默认配置
    return loadConfigs()
}

// ─── 保存配置 ───────────────────────────────────

private suspend fun saveConfig(item: ConfigItem) {
    // TODO: 保存到本地文件系统
    // 格式: KEY=VALUE
}

private suspend fun saveAllConfigs(configs: List<ConfigItem>) {
    // TODO: 批量保存
}

// ─── UI 组件 ───────────────────────────────────

@Composable
private fun ConfigGroupHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ConfigItemCard(
    item: ConfigItem,
    onUpdate: (ConfigItem) -> Unit
) {
    var localValue by remember { mutableStateOf(item.value) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 标题行
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.key,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.required) {
                        Text(
                            text = "必填",
                            fontSize = 10.sp,
                            color = Color.Red,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // 展开/收起按钮
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(if (isExpanded) "▲" else "▼")
                }
            }

            // 描述
            if (isExpanded) {
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 输入控件
            ConfigInputField(
                item = item,
                value = localValue,
                onValueChange = { newValue ->
                    localValue = newValue
                    item.value = newValue
                    onUpdate(item)
                }
            )
        }
    }
}

@Composable
private fun ConfigInputField(
    item: ConfigItem,
    value: String,
    onValueChange: (String) -> Unit
) {
    when (item.type) {
        ConfigType.TEXT, ConfigType.URL -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(item.key) },
                singleLine = true,
                visualTransformation = if (item.isSecret) PasswordVisualTransformation() else VisualTransformation.None
            )
        }
        ConfigType.SECRET -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(item.key) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }
        ConfigType.NUMBER -> {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(item.key) },
                singleLine = true
            )
        }
        ConfigType.BOOLEAN -> {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = item.key)
                Switch(
                    checked = value.toBoolean(),
                    onCheckedChange = { checked -> onValueChange(checked.toString()) }
                )
            }
        }
        ConfigType.DROPDOWN -> {
            // TODO: 下拉选择
        }
    }
}

@Composable
private fun ConfigActions(
    onSave: () -> Unit,
    onReset: () -> Unit,
    onSync: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f)
        ) {
            Text("重置")
        }

        OutlinedButton(
            onClick = onSync,
            modifier = Modifier.weight(1f)
        ) {
            Text("同步代码")
        }

        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f)
        ) {
            Text("保存")
        }
    }
}