package com.pocketagent.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * 首页 - 主入口卡片式布局
 * 
 * 四大核心入口:
 * 1. 对话入口 (主 Agent 功能)
 * 2. 终端入口 (Termux 集成)
 * 3. 配置入口 (主项目配置管理)
 * 4. 悬浮窗入口 (悬浮窗控制)
 */
@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        // 标题区
        HomeHeader()

        // 主功能卡片区 (2x2 网格)
        HomeGrid(navController)

        // 底部状态栏
        HomeStatusBar()
    }
}

@Composable
private fun HomeHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        Text(
            text = "Pocket Agent",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "轻量级移动端 AI 代理",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun HomeGrid(navController: NavController) {
    val cards = listOf(
        HomeCard(
            title = "对话",
            subtitle = "主 Agent 功能",
            description = "执行手机操控、问答、任务规划等完整功能",
            iconRes = "💬",
            color = Color(0xFF4CAF50),
            route = "chat"
        ),
        HomeCard(
            title = "终端",
            subtitle = "Termux 集成",
            description = "执行 Termux 命令、系统操作、环境感知",
            iconRes = "🖥️",
            color = Color(0xFF2196F3),
            route = "terminal"
        ),
        HomeCard(
            title = "配置",
            subtitle = "主项目配置管理",
            description = "管理主 Agent 和子 Agent 的模型配置",
            iconRes = "⚙️",
            color = Color(0xFFFF9800),
            route = "config"
        ),
        HomeCard(
            title = "悬浮窗",
            subtitle = "后台运行控制",
            description = "控制悬浮窗显示、隐藏、查看执行状态",
            iconRes = "🔍",
            color = Color(0xFF9C27B0),
            route = "overlay"
        )
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 第一行
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            cards[0].CardView(navController)
            cards[1].CardView(navController)
        }
        
        // 第二行
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            cards[2].CardView(navController)
            cards[3].CardView(navController)
        }
    }
}

data class HomeCard(
    val title: String,
    val subtitle: String,
    val description: String,
    val iconRes: String,
    val color: Color,
    val route: String
)

@Composable
private fun HomeCard.CardView(navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { navController.navigate(route) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部图标 + 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = iconRes,
                    fontSize = 32.sp
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // 描述
            Text(
                text = description,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun HomeStatusBar() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Agent 状态: 就绪", fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = Color(0xFF4CAF50), shape = RoundedCornerShape(50))
            )
        }
    }
}