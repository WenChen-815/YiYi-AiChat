package com.wenchen.yiyi.feature.worldBook.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wenchen.yiyi.core.database.entity.YiYiWorldBook
import com.wenchen.yiyi.core.designSystem.component.SpaceVerticalXSmall
import com.wenchen.yiyi.core.designSystem.theme.DarkGray
import com.wenchen.yiyi.core.designSystem.theme.LightGray
import com.wenchen.yiyi.feature.worldBook.viewmodel.WorldBookListViewModel
import com.wenchen.yiyi.navigation.routes.WorldBookRoutes

@Composable
internal fun WorldBookListRoute(
    viewModel: WorldBookListViewModel = hiltViewModel(),
    navController: NavController
) {
        WorldBookListScreen(viewModel, navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorldBookListScreen(
    viewModel: WorldBookListViewModel = hiltViewModel(),
    navController: NavController
) {
    val worldBooks by viewModel.worldBooks.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("世界列表") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.navigate(WorldBookRoutes.WorldBookEdit("", true))
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Text("创建")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            if (worldBooks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无世界书，请创建新的世界书")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(worldBooks, key = { it.id }) { worldBook ->
                        WorldBookItem(
                            worldBook = worldBook,
                            onItemClick = {
                                viewModel.navigate(WorldBookRoutes.WorldBookEdit(worldId = worldBook.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorldBookItem(
    worldBook: YiYiWorldBook,
    onItemClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSystemInDarkTheme()) DarkGray else LightGray.copy(alpha = 0.5f))
            .clickable { onItemClick() }
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = worldBook.name ?: "未命名",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            SpaceVerticalXSmall()

            Text(
                text = worldBook.description ?: "无描述",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
