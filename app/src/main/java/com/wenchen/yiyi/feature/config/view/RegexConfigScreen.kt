package com.wenchen.yiyi.feature.config.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.wenchen.yiyi.core.designSystem.theme.*
import com.wenchen.yiyi.core.database.entity.YiYiRegexGroup
import com.wenchen.yiyi.core.designSystem.component.SettingTextFieldItem
import com.wenchen.yiyi.feature.config.viewmodel.RegexConfigViewModel

@Composable
internal fun RegexConfigRoute(
    viewModel: RegexConfigViewModel = hiltViewModel()
) {
    RegexConfigScreen(
        regexGroups = viewModel.regexGroups.collectAsState().value,
        onBack = { viewModel.navigateBack() },
        onNavigateToDetail = { groupId, groupName -> viewModel.navigateToDetail(groupId, groupName) },
        onAddGroup = { name, desc -> viewModel.addGroup(name, desc) },
        onDeleteGroup = { viewModel.deleteGroup(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexConfigScreen(
    regexGroups: List<YiYiRegexGroup>,
    onBack: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    onAddGroup: (String, String) -> Unit,
    onDeleteGroup: (YiYiRegexGroup) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<YiYiRegexGroup?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("正则组") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "返回",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Pink,
                contentColor = WhiteText
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建分组")
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) BlackBg else WhiteBg)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(regexGroups, key = { it.id }) { group ->
                RegexGroupItem(
                    group = group,
                    onClick = { onNavigateToDetail(group.id, group.name ?: "未命名分组") },
                    onDelete = { groupToDelete = group }
                )
            }
        }
    }

    if (showAddDialog) {
        AddGroupDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, desc ->
                onAddGroup(name, desc)
                showAddDialog = false
            }
        )
    }

    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除分组 \"${groupToDelete?.name ?: ""}\" 吗？此操作不可撤销，且会删除该组下的所有规则。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        groupToDelete?.let(onDeleteGroup)
                        groupToDelete = null
                    }
                ) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun RegexGroupItem(
    group: YiYiRegexGroup,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) DarkGray else LightGray
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name ?: "未命名分组",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSystemInDarkTheme()) WhiteText else BlackText
                )
                if (!group.description.isNullOrBlank()) {
                    Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.2f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
            }
        }
    }
}

@Composable
fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建分组") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingTextFieldItem(
                    value = name,
                    onValueChange = { name = it },
                    label = "分组名称",
                    singleLine = true,
                    placeholder = { Text("请输入分组名称") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, desc) },
                enabled = name.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun RegexConfigScreenPreview() {
    AppTheme {
        RegexConfigScreen(
            regexGroups = listOf(
                YiYiRegexGroup(id = "1", name = "净化回复", description = "用于过滤AI回复中的无关内容"),
                YiYiRegexGroup(id = "2", name = "格式化", description = "统一输出格式")
            ),
            onBack = {},
            onNavigateToDetail = { _, _ -> },
            onAddGroup = { _, _ -> },
            onDeleteGroup = {}
        )
    }
}
