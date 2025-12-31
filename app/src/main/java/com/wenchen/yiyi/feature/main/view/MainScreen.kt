package com.wenchen.yiyi.feature.main.view

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.wenchen.yiyi.core.common.components.IconWithText
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.common.theme.Gold
import com.wenchen.yiyi.core.common.theme.GrayText
import com.wenchen.yiyi.core.common.theme.Pink
import com.wenchen.yiyi.core.common.theme.WhiteBg
import com.wenchen.yiyi.core.common.theme.WhiteText
import com.wenchen.yiyi.core.common.utils.StatusBarUtil
import com.wenchen.yiyi.feature.main.viewmodel.MainViewModel

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
    viewModel : MainViewModel = hiltViewModel(),
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
        StatusBarUtil.setStatusBarTextColor(activity as ComponentActivity, false)
    } else {
        StatusBarUtil.setStatusBarTextColor(activity as ComponentActivity, true)
    }
    var showAddPopup by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            when (currentPosition) {
                0 ->
                    TopAppBar(
                        title = {
                            Text(
                                text = "角色列表",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    MaterialTheme.colorScheme.onBackground,
                                ),
                            )
                        },
                        actions = {
//                                TextButton(onClick = {
//                                    val intent = Intent(
//                                        this@MainActivity,
//                                        ConfigActivity::class.java
//                                    )
//                                    startActivity(intent)
//                                }) {
//                                    Text("设置")
//                                }
                            IconWithText(
                                icon = Icons.Rounded.Book,
                                iconTint = MaterialTheme.colorScheme.primary,
                                text = "世界",
                                modifier = Modifier.clickable{
                                    // TODO 跳转世界书籍列表页面
                                }
                                    .padding(horizontal = 8.dp),
                                iconModifier = Modifier.size(24.dp),
                                textModifier = Modifier.padding(top = 4.dp),
                                textStyle = MaterialTheme.typography.labelSmall.copy(MaterialTheme.colorScheme.primary,)
                            )
                            IconWithText(
                                icon = Icons.Rounded.Settings,
                                iconTint = MaterialTheme.colorScheme.primary,
                                text = "设置",
                                modifier = Modifier.clickable{
                                    // TODO 跳转设置页面
                                }
                                    .padding(horizontal = 8.dp),
                                iconModifier = Modifier.size(24.dp),
                                textModifier = Modifier.padding(top = 4.dp),
                                textStyle = MaterialTheme.typography.labelSmall.copy(MaterialTheme.colorScheme.primary,)
                            )
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                    )

                1 ->
                    TopAppBar(
                        title = {
                            Text(
                                text = "聊天列表",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    MaterialTheme.colorScheme.onBackground,
                                ),
                            )
                        },
                        actions = {
                            TextButton(onClick = {
                                // TODO 跳转创建群组页面
                            }) {
                                Text("创建群组")
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                    )
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
                modifier =
                    Modifier
//                        .background(MaterialTheme.colorScheme.primary) // 这样设置是无效的，应该用containerColor
                        .fillMaxWidth()
                        .height(66.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight().weight(1f).clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ){ currentPosition = 0 },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "角色",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = when(currentPosition) {
                                    0 -> FontWeight.Bold
                                    else -> FontWeight.Normal
                                }
                            ),
                            color = if (currentPosition == 0) MaterialTheme.colorScheme.onBackground else GrayText,
                        )
                    }

                    // 中间新增按钮
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新增",
                        tint = WhiteText,
                        modifier =
                            Modifier
                                .size(48.dp, 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    brush =
                                        Brush.horizontalGradient(
                                            colors = listOf(Pink, Gold),
                                        ),
                                )
                                .clickable { showAddPopup = true },
                    )
                    // 弹出菜单
                    if (showAddPopup) {
                        Popup(
                            alignment = Alignment.TopCenter,
                            onDismissRequest = { showAddPopup = false }
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(bottom = 48.dp)
                                    .width(150.dp),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors().copy(
                                    containerColor = WhiteBg
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    // 新增角色选项
                                    Text(
                                        text = "新增角色",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showAddPopup = false
                                                // TODO 跳转角色编辑页面
                                            }
                                            .padding(12.dp),
                                        style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.primary)
                                    )

                                    HorizontalDivider()

                                    // 新增世界选项
                                    Text(
                                        text = "新增世界",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showAddPopup = false
                                                // TODO 跳转世界书籍创建页面
                                            }
                                            .padding(12.dp),
                                        style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxHeight().weight(1f).clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ){ currentPosition = 1 },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "聊天",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = when(currentPosition) {
                                    1 -> FontWeight.Bold
                                    else -> FontWeight.Normal
                                }
                            ),
                            color = if (currentPosition == 1) MaterialTheme.colorScheme.onBackground else GrayText,
                        )
                    }
                }
            }
        },
//            floatingActionButton = {
//                FloatingActionButton(
//                    onClick = {
//                        startActivity(Intent(this@MainActivity, TestActivity::class.java))
//                    },
//                    containerColor = Pink,
//                    modifier = Modifier.padding(16.dp)
//                ) {
//                    Text(
//                        text = "测试区域",
//                        style = MaterialTheme.typography.titleMedium.copy(
//                            fontWeight = FontWeight.Bold
//                        ),
//                        color = Color.White,
//                    )
//                }
//            }
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
        ) {
            when (currentPosition) {
                0 -> HomeScreen(LocalContext.current)
                1 -> ConversationListScreen(LocalContext.current)
            }
        }
    }

//    val models = viewModel.models.collectAsState(emptyList())
//    val uiState = viewModel.uiState.collectAsState(BaseNetWorkUiState.Loading)
//    Scaffold { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//        ) {
//            // 添加按钮触发网络请求
//            Button(
//                onClick = { viewModel.executeGetModels() },
//                modifier = Modifier
//                    .padding(16.dp)
//            ) {
//                Text(text = "发起网络请求")
//            }
//            // 显示加载状态
//            if (uiState.value is BaseNetWorkUiState.Loading) {
//                androidx.compose.material3.CircularProgressIndicator(
//                    modifier = Modifier
//                        .padding(16.dp)
//                        .align(Alignment.CenterHorizontally)
//                )
//            }
//
//            // 显示错误状态
//            if (uiState.value is BaseNetWorkUiState.Error) {
//                Text(
//                    text = "加载失败",
//                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
//                    modifier = Modifier.padding(16.dp)
//                )
//            }
//
//            // 显示模型列表数据
//            if (models.value.isNotEmpty()) {
//                androidx.compose.foundation.lazy.LazyColumn {
//                    items(models.value.size) { index ->
//                        val model = models.value[index]
//                        Text(
//                            text = "ID: ${model.id}, Owner: ${model.owned_by}",
//                            modifier = Modifier
//                                .padding(16.dp)
//                        )
//                    }
//                }
//            } else if (uiState.value is BaseNetWorkUiState.Success && models.value.isEmpty()) {
//                Text(
//                    text = "暂无数据",
//                    modifier = Modifier
//                        .padding(16.dp)
//                )
//            }
//        }
//    }
}

/**
 * 主页面界面浅色主题预览
 */
@Preview(showBackground = true)
@Composable
internal fun MainScreenPreview() {
    AIChatTheme {
        MainScreen(
        )
    }
}

/**
 * 主页面界面深色主题预览
 */
@Preview(showBackground = true)
@Composable
internal fun MainScreenPreviewDark() {
    AIChatTheme(darkTheme = true) {
        MainScreen(
        )
    }
}
