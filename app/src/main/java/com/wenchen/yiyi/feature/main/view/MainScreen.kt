package com.wenchen.yiyi.feature.main.view

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ArrowCircleUp
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.wenchen.yiyi.core.designSystem.theme.AppTheme
import com.wenchen.yiyi.core.designSystem.theme.Gold
import com.wenchen.yiyi.core.designSystem.theme.GrayText
import com.wenchen.yiyi.core.designSystem.theme.Pink
import com.wenchen.yiyi.core.util.ui.StatusBarUtils
import com.wenchen.yiyi.feature.main.viewmodel.MainViewModel
import com.wenchen.yiyi.navigation.routes.AiChatRoutes
import com.wenchen.yiyi.navigation.routes.ConfigRoutes
import com.wenchen.yiyi.navigation.routes.OutputRoutes
import com.wenchen.yiyi.navigation.routes.WorldBookRoutes
import kotlinx.coroutines.launch

/**
 * 主页面路由
 *
 * @param viewModel 主页面 ViewModel
 */
@Composable
internal fun MainRoute(
    viewModel: MainViewModel = hiltViewModel(),
    navController: NavController
) {
    MainScreen(
        viewModel = viewModel,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    navController: NavController = NavController(LocalContext.current)
) {
    MainScreenContent(
        viewModel = viewModel,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MainScreenContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val activity = LocalActivity.current
    if (isSystemInDarkTheme()) {
        StatusBarUtils.setStatusBarTextColor(activity as ComponentActivity, false)
    } else {
        StatusBarUtils.setStatusBarTextColor(activity as ComponentActivity, true)
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var showAddPopup by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (pagerState.currentPage == 0) "角色列表" else "聊天列表",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                actions = {
                    if (pagerState.currentPage == 0) {
                        Row {
                            IconButton(onClick = { viewModel.navigate(OutputRoutes.Output) }) {
                                Icon(Icons.Rounded.ArrowCircleUp, contentDescription = "导出", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.navigate(WorldBookRoutes.WorldBookList) }) {
                                Icon(Icons.Rounded.Book, contentDescription = "世界", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.navigate(ConfigRoutes.Settings) }) {
                                Icon(Icons.Rounded.Settings, contentDescription = "设置", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        TextButton(onClick = {
                            viewModel.navigate(AiChatRoutes.ConversationEdit("", true))
                        }) {
                            Text("创建群组")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(64.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Character Tab
                    NavigationTabItem(
                        selected = pagerState.currentPage == 0,
                        icon = Icons.Rounded.PersonOutline,
                        selectedIcon = Icons.Rounded.Person,
                        label = "角色",
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                    )

                    // Middle Add Button
                    Box(contentAlignment = Alignment.Center) {
                        FloatingActionButton(
                            onClick = { showAddPopup = true },
                            shape = CircleShape,
                            containerColor = Color.Transparent,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp),
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.horizontalGradient(listOf(Pink, Gold)),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "新增", tint = Color.White)
                        }

                        if (showAddPopup) {
                            Popup(
                                alignment = Alignment.TopCenter,
                                onDismissRequest = { showAddPopup = false }
                            ) {
                                Card(
                                    modifier = Modifier
                                        .padding(bottom = 72.dp)
                                        .width(160.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column {
                                        PopupMenuItem("新增角色") {
                                            showAddPopup = false
                                            viewModel.navigate(AiChatRoutes.CharacterEdit("", true))
                                        }
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                                        PopupMenuItem("新增世界") {
                                            showAddPopup = false
                                            viewModel.navigate(WorldBookRoutes.WorldBookEdit("", true))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Chat Tab
                    NavigationTabItem(
                        selected = pagerState.currentPage == 1,
                        icon = Icons.Rounded.ChatBubbleOutline,
                        selectedIcon = Icons.Rounded.ChatBubble,
                        label = "聊天",
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> HomeScreen(LocalContext.current)
                1 -> ConversationListScreen(LocalContext.current)
            }
        }
    }
}

@Composable
private fun RowScope.NavigationTabItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else GrayText,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else GrayText
        )
    }
}

@Composable
private fun PopupMenuItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Preview(showBackground = true)
@Composable
internal fun MainScreenPreview() {
    AppTheme {
        MainScreen()
    }
}
