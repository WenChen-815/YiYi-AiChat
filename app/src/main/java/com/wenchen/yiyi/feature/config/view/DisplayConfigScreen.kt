package com.wenchen.yiyi.feature.config.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.wenchen.yiyi.core.designSystem.theme.AppTheme
import com.wenchen.yiyi.core.designSystem.theme.TextGray
import com.wenchen.yiyi.feature.aiChat.common.ChatLayoutMode
import com.wenchen.yiyi.feature.aiChat.common.LayoutType
import com.wenchen.yiyi.feature.config.viewmodel.DisplayConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DisplayConfigRoute(
    viewModel: DisplayConfigViewModel = hiltViewModel(),
    navController: NavController
) {
    val userConfig by viewModel.userConfig.collectAsState()
    val layoutMode = userConfig?.layoutMode ?: ChatLayoutMode()

    DisplayConfigScreenContent(
        layoutMode = layoutMode,
        onBackClick = { viewModel.navigateBack() },
        onRestoreDefault = { viewModel.restoreDefault() },
        onUpdateLayoutMode = { mode, userBubble, userText, aiBubble, aiText ->
            viewModel.updateLayoutMode(mode, userBubble, userText, aiBubble, aiText)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayConfigScreenContent(
    layoutMode: ChatLayoutMode,
    onBackClick: () -> Unit,
    onRestoreDefault: () -> Unit,
    onUpdateLayoutMode: (LayoutType?, Color?, Color?, Color?, Color?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("显示设置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "返回",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onRestoreDefault) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("恢复默认")
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "* 当前显示设置仅支持单聊使用，群聊中不生效",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "布局模式",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LayoutType.entries.forEach { type ->
                val isSelected = layoutMode.type == type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUpdateLayoutMode(type, null, null, null, null) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onUpdateLayoutMode(type, null, null, null, null) },
                        enabled = type != LayoutType.FULL_EXTENDED
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when (type) {
                                LayoutType.STANDARD -> "标准模式"
                                LayoutType.ONLY_BUBBLE -> "仅气泡模式"
                                LayoutType.FULL -> "沉浸模式"
                                LayoutType.FULL_EXTENDED -> "完全沉浸模式"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = when (type) {
                                LayoutType.STANDARD -> "显示头像和气泡，较强的对话感"
                                LayoutType.ONLY_BUBBLE -> "无头像，仅显示气泡"
                                LayoutType.FULL -> "聊天气泡极小边距，增强沉浸"
                                LayoutType.FULL_EXTENDED -> "全屏沉浸（待实现）"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "气泡颜色",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ColorSettingItem(
                label = "用户气泡颜色",
                color = layoutMode.userBubbleColor,
                onColorChange = { onUpdateLayoutMode(null, it, null, null, null) }
            )

            ColorSettingItem(
                label = "用户文字颜色",
                color = layoutMode.userTextColor,
                onColorChange = { onUpdateLayoutMode(null, null, it, null, null) }
            )

            ColorSettingItem(
                label = "AI气泡颜色",
                color = layoutMode.aiBubbleColor,
                onColorChange = { onUpdateLayoutMode(null, null, null, it, null) }
            )

            ColorSettingItem(
                label = "AI文字颜色",
                color = layoutMode.aiTextColor,
                onColorChange = { onUpdateLayoutMode(null, null, null, null, it) }
            )
        }
    }
}

@Composable
fun ColorSettingItem(
    label: String,
    color: Color,
    onColorChange: (Color) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        SimpleColorPickerDialog(
            initialColor = color,
            onDismiss = { showDialog = false },
            onColorSelected = {
                onColorChange(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun SimpleColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color(0xFF6200EE), Color(0xFF3700B3), Color(0xFF03DAC6), Color(0xFF018786),
        Color(0xFFBB86FC), Color(0xFFCF6679), Color(0xFF000000), Color(0xFFFFFFFF),
        Color(0xFF808080), Color(0xFFE0E0E0), Color(0xFFFF5722), Color(0xFF4CAF50)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (color == initialColor) 3.dp else 1.dp,
                                    if (color == initialColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                                .clickable { onColorSelected(color) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DisplayConfigScreenPreview() {
    AppTheme {
        DisplayConfigScreenContent(
            layoutMode = ChatLayoutMode(),
            onBackClick = {},
            onRestoreDefault = {},
            onUpdateLayoutMode = { _, _, _, _, _ -> }
        )
    }
}
