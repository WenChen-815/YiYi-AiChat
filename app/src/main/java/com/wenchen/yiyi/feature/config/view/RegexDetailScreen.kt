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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenchen.yiyi.core.designSystem.theme.*
import com.wenchen.yiyi.core.database.entity.YiYiRegexScript
import com.wenchen.yiyi.core.designSystem.component.SpaceHorizontalSmall
import com.wenchen.yiyi.core.designSystem.component.SpaceHorizontalXSmall
import com.wenchen.yiyi.core.designSystem.component.SpaceVerticalSmall
import com.wenchen.yiyi.core.ui.SettingTextFieldItem
import com.wenchen.yiyi.core.ui.SwitchWithText
import com.wenchen.yiyi.feature.config.viewmodel.RegexDetailViewModel

@Composable
internal fun RegexDetailRoute(
    viewModel: RegexDetailViewModel = hiltViewModel()
) {
    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    val scripts by viewModel.regexScripts.collectAsStateWithLifecycle()

    RegexDetailScreen(
        groupId = viewModel.groupId,
        groupName = groupName,
        scripts = scripts,
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
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.3f
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(
                            text = "分组名称",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " * ",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        SpaceHorizontalSmall()
                        TextField(
                            value = groupName,
                            onValueChange = onGroupNameChange,
                            placeholder = { Text("为世界书命名", color = Color.Gray) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "规则列表",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "添加",
                            modifier = Modifier.clickable {
                                editingScript = YiYiRegexScript(id = "", groupId = groupId)
                                showEditDialog = true
                            },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f)) {
                    val isEnabled = script.disabled != true
                    Text(
                        text = if (isEnabled) "已启用" else "已禁用",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .background(
                                shape = RoundedCornerShape(4.dp),
                                color = (if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error).copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    SpaceHorizontalXSmall()
                    Text(
                        text = script.scriptName ?: "未命名规则",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSystemInDarkTheme()) TextPrimaryDark else TextPrimaryLight
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        "编辑",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
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

            SpaceVerticalSmall()

            Text(
                text = "查找: ${script.findRegex}",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                maxLines = 1
            )
            Text(
                text = "替换: ${script.replaceString}",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
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
                    modifier = Modifier.heightIn(max = 300.dp),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(
                        onClick = {
                            onConfirm(script.copy(
                                scriptName = name,
                                findRegex = findRegex,
                                replaceString = replaceString,
                                disabled = disabled
                            ))
                        }
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegexDetailScreenPreview() {
    AppTheme {
        RegexDetailScreen(
            groupId = "1",
            groupName = "我的正则组",
            scripts = listOf(
                YiYiRegexScript(id = "1", groupId = "1", scriptName = "净化", findRegex = ".*", replaceString = ""),
                YiYiRegexScript(id = "2", groupId = "1", scriptName = "格式化", findRegex = "\\s+", replaceString = " ", disabled = true)
            ),
            onBack = {},
            onGroupNameChange = {},
            onAddOrUpdateScript = {},
            onDeleteScript = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegexScriptEditDialogPreview(){
    AppTheme {
        RegexScriptEditDialog(
            script = YiYiRegexScript(id = "1", groupId = "1", scriptName = "净化", findRegex = ".*", replaceString = "relaceString"),
            onDismiss = {},
            onConfirm = {}
        )
    }
}
