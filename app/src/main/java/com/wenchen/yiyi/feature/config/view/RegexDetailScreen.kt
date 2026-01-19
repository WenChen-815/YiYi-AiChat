package com.wenchen.yiyi.feature.config.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
        scripts = viewModel.regexScripts.collectAsState().value,
        onBack = { viewModel.navigateBack() },
        onAddOrUpdateScript = { viewModel.addOrUpdateScript(it) },
        onDeleteScript = { viewModel.deleteScript(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexDetailScreen(
    groupId: String,
    scripts: List<YiYiRegexScript>,
    onBack: () -> Unit,
    onAddOrUpdateScript: (YiYiRegexScript) -> Unit,
    onDeleteScript: (YiYiRegexScript) -> Unit
) {
    var editingScript by remember { mutableStateOf<YiYiRegexScript?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("正则详情") },
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
            items(scripts, key = { it.id }) { script ->
                RegexScriptItem(
                    script = script,
                    onEdit = {
                        editingScript = script
                        showEditDialog = true
                    },
                    onDelete = { onDeleteScript(script) }
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = script.scriptName ?: "未命名规则",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(text = "查找: ${script.findRegex}", style = MaterialTheme.typography.bodySmall, color = GrayText)
            Text(text = "替换: ${script.replaceString}", style = MaterialTheme.typography.bodySmall, color = GrayText)
            
            if (script.disabled == true) {
                Text(
                    text = "已禁用",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
                            onConfirm(script.copy(
                                scriptName = name,
                                findRegex = findRegex,
                                replaceString = replaceString,
                                disabled = disabled,
                                markdownOnly = markdownOnly
                            ))
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
            onAddOrUpdateScript = {},
            onDeleteScript = {}
        )
    }
}
