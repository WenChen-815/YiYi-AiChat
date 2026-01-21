package com.wenchen.yiyi.feature.worldBook.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.database.entity.WorldBookEntryExtensions
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookEntry
import com.wenchen.yiyi.feature.worldBook.viewmodel.WorldBookEntryEditViewModel
import kotlinx.coroutines.launch

@Composable
internal fun WorldBookEntryEditRoute(
    viewModel: WorldBookEntryEditViewModel = hiltViewModel(),
    navController: NavController
) {
    val entry by viewModel.entryState.collectAsStateWithLifecycle()
    val showDiscardDialog by viewModel.showDiscardDialog.collectAsStateWithLifecycle()

    WorldBookEntryEditContent(
        entry = entry,
        showDiscardDialog = showDiscardDialog,
        onBack = { viewModel.handleBack() },
        onUpdateEntry = { updater -> viewModel.updateState(updater) },
        onDiscardAndExit = { viewModel.discardAndExit() },
        onDismissDiscardDialog = { viewModel.dismissDiscardDialog() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorldBookEntryEditContent(
    entry: YiYiWorldBookEntry,
    showDiscardDialog: Boolean,
    onBack: () -> Unit,
    onUpdateEntry: ((YiYiWorldBookEntry) -> YiYiWorldBookEntry) -> Unit,
    onDiscardAndExit: () -> Unit,
    onDismissDiscardDialog: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    /*
     * rememberCoroutineScope() 返回一个与当前 Composition（组合）生命周期绑定的 CoroutineScope
     * 生命周期感知：当 Composable 函数离开组合时，该作用域会自动取消所有正在运行的协程
     * 内存安全：防止因 Activity 或 Fragment 销毁后协程仍在运行而导致的内存泄漏
     */
    val scope = rememberCoroutineScope()
    var showStrategySheet by remember { mutableStateOf(false) }
    var showProbabilityDialog by remember { mutableStateOf(false) }

    // 使用本地状态管理关键词字符串，防止 joinToString 导致的末尾分隔符丢失
    var keysInputText by remember { mutableStateOf(entry.keys.joinToString("、")) }

    // 当外部 entry.keys 发生剧烈变化时同步本地状态（例如初始化加载）
    // 增加逻辑判断防止光标因重复赋值跳动
    LaunchedEffect(entry.keys) {
        val currentString = entry.keys.joinToString("、")
        if (currentString != keysInputText && !keysInputText.endsWith("、") && !keysInputText.endsWith("，") && !keysInputText.endsWith(",")) {
            keysInputText = currentString
        }
    }

    // 拦截系统返回键
    BackHandler {
        onBack()
    }

    // 丢弃确认对话框
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDismissDiscardDialog,
            title = { Text("丢弃更改？") },
            text = { Text("条目信息不完整（需要名字和内容），返回将丢弃已编辑的内容。") },
            confirmButton = {
                TextButton(onClick = onDiscardAndExit) {
                    Text("丢弃并退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDiscardDialog) {
                    Text("继续编辑")
                }
            }
        )
    }

    // 概率设置对话框
    if (showProbabilityDialog) {
        var tempProbability by remember { mutableStateOf(entry.extensions.probability.toString()) }
        AlertDialog(
            onDismissRequest = { showProbabilityDialog = false },
            title = { Text("设置激活概率") },
            text = {
                Column {
                    Text("请输入 0-100 之间的数字")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempProbability,
                        onValueChange = { input ->
                            if (input.isEmpty() || (input.all { it.isDigit() } && input.toInt() in 0..100)) {
                                tempProbability = input
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = tempProbability.toIntOrNull() ?: 100
                    onUpdateEntry { it.copy(extensions = it.extensions.copy(probability = value)) }
                    showProbabilityDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProbabilityDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 策略选择抽屉
    if (showStrategySheet) {
        ModalBottomSheet(
            onDismissRequest = { showStrategySheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                ListItem(
                    headlineContent = { Text("常驻") },
                    modifier = Modifier.clickable {
                        onUpdateEntry { it.copy(constant = true) }
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showStrategySheet = false
                        }
                    },
                    trailingContent = {
                        if (entry.constant) Icon(Icons.Rounded.ArrowBackIosNew, null, modifier = Modifier.size(18.dp))
                    }
                )
                ListItem(
                    headlineContent = { Text("关键词") },
                    modifier = Modifier.clickable {
                        onUpdateEntry { it.copy(constant = false) }
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showStrategySheet = false
                        }
                    },
                    trailingContent = {
                        if (!entry.constant) Icon(Icons.Rounded.ArrowBackIosNew, null, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("编辑条目", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "返回", modifier = Modifier.size(18.dp))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 添加 imePadding 和 navigationBarsPadding，确保键盘弹出时内容自动上移
                .padding(innerPadding)
                .imePadding() 
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 名字部分
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("名字", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(" * ", color = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = entry.name ?: "",
                        onValueChange = { newName ->
                            onUpdateEntry { currentEntry -> currentEntry.copy(name = newName) }
                        },
                        placeholder = { Text("条目名称", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // 内容部分
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row {
                        Text("内容", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(" * ", color = Color.Red)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = entry.content ?: "",
                        onValueChange = { newContent ->
                            onUpdateEntry { currentEntry -> currentEntry.copy(content = newContent) }
                        },
                        placeholder = { Text("条目被激活时，将注入到提示词中的内容", color = Color.Gray, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                }
            }

            // 激活设置项
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EntrySettingRow(
                    label = "激活策略",
                    value = if (entry.constant) "常驻" else "关键词",
                    onClick = { showStrategySheet = true }
                )
                EntrySettingRow(
                    label = "激活概率",
                    value = "${entry.extensions.probability}%",
                    onClick = { showProbabilityDialog = true }
                )
            }

            // 关键词部分
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("关键词", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        TextField(
                            value = keysInputText,
                            onValueChange = { input ->
                                // 同步本地 UI 文本，允许输入分隔符
                                keysInputText = input
                                // 只有当输入不以分隔符结尾时，才解析并同步到外部 List 状态
                                val newKeys = input.split("、", "，", ",").map { it.trim() }.filter { it.isNotEmpty() }
                                onUpdateEntry { it.copy(keys = newKeys) }
                            },
                            placeholder = { Text("请输入关键词，多个关键词用“、”隔开", color = Color.Gray, fontSize = 14.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun EntrySettingRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 14.sp, color = Color.Gray)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "条目编辑预览")
@Composable
private fun WorldBookEntryEditPreview() {
    AIChatTheme {
        WorldBookEntryEditContent(
            entry = YiYiWorldBookEntry(
                entryId = "1",
                bookId = "world_1",
                name = "示例条目",
                keys = listOf("关键词A", "关键词B"),
                content = "这是条目的具体内容描述...",
                constant = false,
                extensions = WorldBookEntryExtensions(probability = 100)
            ),
            showDiscardDialog = false,
            onBack = {},
            onUpdateEntry = { _ -> },
            onDiscardAndExit = {},
            onDismissDiscardDialog = {}
        )
    }
}
