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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.wenchen.yiyi.core.database.entity.YiYiWorldBook
import com.wenchen.yiyi.core.designSystem.component.SpaceVerticalXSmall
import com.wenchen.yiyi.core.designSystem.theme.*
import com.wenchen.yiyi.feature.config.viewmodel.WorldBookListViewModel
import com.wenchen.yiyi.navigation.routes.WorldBookRoutes

@Composable
internal fun WorldBookListRoute(
    navController: NavHostController,
    viewModel: WorldBookListViewModel = hiltViewModel()
) {
    val worldBooks by viewModel.worldBooks.collectAsStateWithLifecycle()

    WorldBookListScreen(
        worldBooks = worldBooks,
        onBack = { viewModel.navigateBack() },
        onNavigateToEdit = { worldId ->
            viewModel.navigate(WorldBookRoutes.WorldBookEdit(worldId = worldId))
        },
        onCreateNew = {
            viewModel.navigate(WorldBookRoutes.WorldBookEdit("", true))
        },
        onDeleteWorldBook = { viewModel.deleteWorldBook(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorldBookListScreen(
    worldBooks: List<YiYiWorldBook>,
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onCreateNew: () -> Unit,
    onDeleteWorldBook: (YiYiWorldBook) -> Unit
) {
    var bookToDelete by remember { mutableStateOf<YiYiWorldBook?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("世界书") },
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
                onClick = onCreateNew,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = TextWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "创建世界书")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        if (worldBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "暂无世界书，请点击右下角按钮创建",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(worldBooks, key = { it.id }) { worldBook ->
                    WorldBookItem(
                        worldBook = worldBook,
                        onClick = { onNavigateToEdit(worldBook.id) },
                        onDelete = { bookToDelete = worldBook }
                    )
                }
            }
        }
    }

    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除世界书 \"${bookToDelete?.name ?: ""}\" 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        bookToDelete?.let(onDeleteWorldBook)
                        bookToDelete = null
                    }
                ) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun WorldBookItem(
    worldBook: YiYiWorldBook,
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
                    text = worldBook.name ?: "未命名世界书",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSystemInDarkTheme()) TextPrimaryDark else TextPrimaryLight
                )
                if (!worldBook.description.isNullOrBlank()) {
                    SpaceVerticalXSmall()
                    Text(
                        text = worldBook.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
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

@Preview(showBackground = true)
@Composable
fun WorldBookListScreenPreview() {
    val worldBooks = listOf(
        YiYiWorldBook(
            id = "1",
            name = "云岿山",
            description = "师傅仪玄还有叶瞬光小师姐"
        ),
        YiYiWorldBook(
            id = "2",
            name = "黑童话世界",
            description = "狼妈妈：千万不要给小红帽开门"
        )
    )
    AppTheme {
        WorldBookListScreen(
            worldBooks = worldBooks,
            onBack = {},
            onNavigateToEdit = { worldId -> },
            onCreateNew = {},
            onDeleteWorldBook = {}
        )
    }
}