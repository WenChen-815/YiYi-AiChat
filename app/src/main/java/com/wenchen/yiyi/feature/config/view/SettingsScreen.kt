package com.wenchen.yiyi.feature.config.view

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.wenchen.yiyi.core.designSystem.component.SpaceVerticalLarge
import com.wenchen.yiyi.core.designSystem.theme.DarkGray
import com.wenchen.yiyi.core.designSystem.theme.TextGray
import com.wenchen.yiyi.core.designSystem.theme.LightGray
import com.wenchen.yiyi.feature.config.viewmodel.SettingsViewModel
import com.wenchen.yiyi.navigation.routes.ConfigRoutes
import com.wenchen.yiyi.navigation.routes.RegexRoutes
import com.wenchen.yiyi.navigation.routes.WorldBookRoutes

@Composable
internal fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    SettingsScreen(viewModel = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "返回",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingNavigationCard(
                title = "聊天设置",
                description = "配置用户信息、对话上下文及显示项",
                onClick = { viewModel.navigate(ConfigRoutes.ChatConfig) }
            )

            SpaceVerticalLarge()

            SettingNavigationCard(
                title = "显示设置",
                description = "配置聊天气泡颜色及布局模式",
                onClick = { viewModel.navigate(ConfigRoutes.DisplayConfig) }
            )

            SpaceVerticalLarge()

            SettingNavigationCard(
                title = "API 配置",
                description = "配置 API Key、中转地址及对话模型",
                onClick = { viewModel.navigate(ConfigRoutes.ApiConfig) }
            )

            SpaceVerticalLarge()

            SettingNavigationCard(
                title = "世界书",
                description = "管理世界书",
                onClick = { viewModel.navigate(WorldBookRoutes.WorldBookList) }
            )

            SpaceVerticalLarge()

            SettingNavigationCard(
                title = "正则组",
                description = "管理正则表达式分组及替换规则",
                onClick = { viewModel.navigate(RegexRoutes.RegexConfig) }
            )
        }
    }
}

@Composable
fun SettingNavigationCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) DarkGray else LightGray.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
