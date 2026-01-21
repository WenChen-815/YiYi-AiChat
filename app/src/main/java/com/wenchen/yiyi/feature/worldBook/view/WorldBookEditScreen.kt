package com.wenchen.yiyi.feature.worldBook.view

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.common.theme.Gold
import com.wenchen.yiyi.core.common.theme.Pink
import com.wenchen.yiyi.core.common.theme.WhiteText
import com.wenchen.yiyi.core.database.entity.YiYiWorldBook
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookEntry
import com.wenchen.yiyi.core.util.ui.StatusBarUtils
import com.wenchen.yiyi.feature.worldBook.viewmodel.WorldBookEditViewModel

@Composable
internal fun WorldBookEditRoute(
    viewModel: WorldBookEditViewModel = hiltViewModel(),
    navController: NavController
) {
    val worldBook by viewModel.worldBook.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val isNewWorld by viewModel.isNewWorld.collectAsStateWithLifecycle()

    WorldBookEditScreen(
        isNewWorld = isNewWorld,
        worldBook = worldBook,
        entries = entries,
        onNavigateBack = { viewModel.navigateBack() },
        onDeleteWorldBook = { viewModel.deleteWorldBook() },
        onSaveWorldBook = { name, desc -> viewModel.saveWorldBook(name, desc) },
        onNavigateToEntryEdit = { viewModel.navigateToEntryEdit(it) },
        onToggleEntryEnabled = { entry, enabled -> viewModel.toggleEntryEnabled(entry, enabled) },
        onDeleteEntry = { entry -> viewModel.deleteEntry(entry) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorldBookEditScreen(
    isNewWorld: Boolean,
    worldBook: YiYiWorldBook?,
    entries: List<YiYiWorldBookEntry>,
    onNavigateBack: () -> Unit,
    onDeleteWorldBook: () -> Unit,
    onSaveWorldBook: (String, String) -> Unit,
    onNavigateToEntryEdit: (String?) -> Unit,
    onToggleEntryEnabled: (YiYiWorldBookEntry, Boolean) -> Unit,
    onDeleteEntry: (YiYiWorldBookEntry) -> Unit
) {
    val activity = LocalActivity.current as? ComponentActivity
    if (activity != null) {
        StatusBarUtils.setStatusBarTextColor(activity, !isSystemInDarkTheme())
    }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(worldBook) {
        worldBook?.let {
            name = it.name ?: ""
            description = it.description ?: ""
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个世界吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteWorldBook()
                    showDeleteDialog = false
                }) {
                    Text("确认", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isNewWorld) "新建世界书" else "编辑世界书",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "返回", modifier = Modifier.size(18.dp))
                    }
                },
                actions = {
                    if (!isNewWorld) {
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { onSaveWorldBook(name, description) },
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Pink, Gold)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "保存",
                            color = WhiteText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
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
                    ) {
                        Text(
                            text = "名字",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " * ",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = name,
                            onValueChange = { name = it },
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
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "描述",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " * ",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("描述内容……", color = Color.Gray) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 3
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
                        "条目",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "添加",
                            modifier = Modifier.clickable { onNavigateToEntryEdit(null) },
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(entries, key = { it.entryId }) { entry ->
                WorldBookEntryItem(
                    entry = entry,
                    onEdit = { onNavigateToEntryEdit(entry.entryId) },
                    onToggle = { onToggleEntryEnabled(entry, it) },
                    onDelete = { onDeleteEntry(entry) }
                )
            }

//            item {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 12.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Icon(
//                                Icons.Default.PushPin,
//                                null,
//                                modifier = Modifier.size(14.dp),
//                                tint = Color.LightGray
//                            )
//                            Text("常驻", fontSize = 12.sp, color = Color.LightGray)
//                        }
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Icon(
//                                Icons.Default.Key,
//                                null,
//                                modifier = Modifier.size(14.dp),
//                                tint = Color.LightGray
//                            )
//                            Text("关键词", fontSize = 12.sp, color = Color.LightGray)
//                        }
//                    }
//                }
//            }
        }
    }
}

@Composable
fun WorldBookEntryItem(
    entry: YiYiWorldBookEntry,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        ),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
//            Icon(
//                if (entry.constant) Icons.Default.PushPin else Icons.Default.Key,
//                contentDescription = null,
//                modifier = Modifier.size(16.dp),
//                tint = MaterialTheme.colorScheme.primary
//            )
            Row(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name ?: "未命名条目",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (entry.constant) "常驻" else "关键词",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.constant) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.background(
                        shape = RoundedCornerShape(4.dp),
                        color = if (entry.constant) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ).padding(2.dp)
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Outlined.Edit, "编辑", modifier = Modifier.size(16.dp), tint = Color.Gray)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = entry.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.7f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.width(16.dp))

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

@Preview(showBackground = true)
@Composable
private fun WorldBookEditScreenPreview() {
    AIChatTheme {
        WorldBookEditScreen(
            isNewWorld = false,
            worldBook = YiYiWorldBook(id = "1", name = "我的异世界", description = "这是一个充满魔法的世界"),
            entries = listOf(
                YiYiWorldBookEntry(entryId = "11", bookId = "1", name = "魔法", keys = listOf("魔法"), content = "魔法是基础", constant = true, enabled = true),
                YiYiWorldBookEntry(entryId = "12", bookId = "1", name = "剑士", keys = listOf("剑士"), content = "剑士很强", constant = false, enabled = false)
            ),
            onNavigateBack = {},
            onDeleteWorldBook = {},
            onSaveWorldBook = { _, _ -> },
            onNavigateToEntryEdit = {},
            onToggleEntryEnabled = { _, _ -> },
            onDeleteEntry = {}
        )
    }
}