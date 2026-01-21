package com.wenchen.yiyi.feature.config.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.wenchen.yiyi.core.common.theme.*
import com.wenchen.yiyi.core.database.entity.YiYiRegexScript
import com.wenchen.yiyi.core.designSystem.component.SettingTextFieldItem
import com.wenchen.yiyi.core.designSystem.component.SwitchWithText
import com.wenchen.yiyi.feature.config.viewmodel.RegexDetailViewModel

@Composable
internal fun RegexDetailRoute(
    viewModel: RegexDetailViewModel = hiltViewModel()
) {
    RegexDetailScreen(
        groupId = viewModel.groupId,
        groupName = viewModel.groupName.collectAsState().value,
        scripts = viewModel.regexScripts.collectAsState().value,
        onBack = { viewModel.navigateBack() },
        onGroupNameChange = { viewModel.updateGroupName(it) },
        onAddOrUpdateScript = { viewModel.addOrUpdateScript(it) },
        onDeleteScript = { viewModel.deleteScript(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexDetailScreen(
    groupId: String,
    groupName: String,
    scripts: List<YiYiRegexScript>,
    onBack: () -> Unit,
    onGroupNameChange: (String) -> Unit,
    onAddOrUpdateScript: (YiYiRegexScript) -> Unit,
    onDeleteScript: (YiYiRegexScript) -> Unit
) {
    var editingScript by remember { mutableStateOf<YiYiRegexScript?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var scriptToDelete by remember { mutableStateOf<YiYiRegexScript?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分组详情") },
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
                onClick = {
                    editingScript = YiYiRegexScript(id = "", groupId = groupId)
                    showEditDialog = true
                },
                containerColor = Pink,
                contentColor = WhiteText
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加规则")
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
            item {
                SettingTextFieldItem(
                    value = groupName,
                    onValueChange = onGroupNameChange,
                    label = "分组名称",
                    placeholder = { Text("输入分组名称") },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = if (isSystemInDarkTheme()) GrayBorder else LightGray
                )
                Text(
                    text = "规则列表",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(scripts, key = { it.id }) { script ->
                RegexScriptItem(
                    script = script,
                    onEdit = {
                        editingScript = script
                        showEditDialog = true
                    },
                    onDelete = { scriptToDelete = script }
                )
            }
        }
    }

    if (showEditDialog && editingScript != null) {
        RegexScriptEditDialog(
            script = editingScript!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedScript ->
                onAddOrUpdateScript(updatedScript)
                showEditDialog = false
            }
        )
    }

    if (scriptToDelete != null) {
        AlertDialog(
            onDismissRequest = { scriptToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除规则 \"${scriptToDelete?.scriptName ?: "未命名规则"}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scriptToDelete?.let(onDeleteScript)
                        scriptToDelete = null
                    }
                ) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { scriptToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun RegexScriptItem(
    script: YiYiRegexScript,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) DarkGray else LightGray
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f)) {
                    Text(
                        text = script.scriptName ?: "未命名规则",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = if (script.disabled == true) "已禁用" else "已启用",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (script.disabled == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                shape = RoundedCornerShape(4.dp),
                                color = if (script.disabled == true) MaterialTheme.colorScheme.error.copy(
                                    alpha = 0.2f
                                ) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                            .padding(2.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Outlined.Edit,
                        "编辑",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.2f))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "删除",
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "查找: ${script.findRegex}",
                style = MaterialTheme.typography.bodySmall,
                color = GrayText,
                maxLines = 1
            )
            Text(
                text = "替换: ${script.replaceString}",
                style = MaterialTheme.typography.bodySmall,
                color = GrayText,
                maxLines = 1
            )


        }
    }
}

@Composable
fun RegexScriptEditDialog(
    script: YiYiRegexScript,
    onDismiss: () -> Unit,
    onConfirm: (YiYiRegexScript) -> Unit
) {
    var name by remember { mutableStateOf(script.scriptName ?: "") }
    var findRegex by remember { mutableStateOf(script.findRegex ?: "") }
    var replaceString by remember { mutableStateOf(script.replaceString ?: "") }
    var disabled by remember { mutableStateOf(script.disabled ?: false) }
    var markdownOnly by remember { mutableStateOf(script.markdownOnly ?: false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (script.id.isEmpty()) "添加规则" else "编辑规则",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                SettingTextFieldItem(
                    value = name,
                    onValueChange = { name = it },
                    label = "规则名称",
                    placeholder = { Text("例如：净化回复") }
                )

                SettingTextFieldItem(
                    value = findRegex,
                    onValueChange = { findRegex = it },
                    label = "查找 (Regex)",
                    placeholder = { Text("正则表达式") }
                )

                SettingTextFieldItem(
                    value = replaceString,
                    onValueChange = { replaceString = it },
                    label = "替换为",
                    placeholder = { Text("替换文本") }
                )

                SwitchWithText(
                    checked = !disabled,
                    onCheckedChange = { disabled = !it },
                    text = "启用此规则"
                )

//                SwitchWithText(
//                    checked = markdownOnly,
//                    onCheckedChange = { markdownOnly = it },
//                    text = "仅作用于 Markdown"
//                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消", color = Color.Red) }
                    TextButton(
                        onClick = {
                            onConfirm(
                                script.copy(
                                    scriptName = name,
                                    findRegex = findRegex,
                                    replaceString = replaceString,
                                    disabled = disabled,
                                    markdownOnly = markdownOnly
                                )
                            )
                        },
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegexDetailScreenPreview() {
    AIChatTheme {
        RegexDetailScreen(
            groupId = "1",
            groupName = "正则分组",
            scripts = listOf(
                YiYiRegexScript(
                    id = "1",
                    groupId = "1",
                    scriptName = "移除思考过程",
                    findRegex = "<thought>[\\\\s\\\\S]*?</thought>",
                    replaceString = "",
                    disabled = false
                ),
                YiYiRegexScript(
                    id = "2",
                    groupId = "1",
                    scriptName = "隐藏特定词汇",
                    findRegex = "敏感词",
                    replaceString = "***",
                    disabled = true
                )
            ),
            onBack = {},
            onGroupNameChange = {},
            onAddOrUpdateScript = {},
            onDeleteScript = {}
        )
    }
}
