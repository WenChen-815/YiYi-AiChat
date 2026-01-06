package com.wenchen.yiyi.feature.aiChat.view

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.wenchen.yiyi.core.designSystem.component.DisplayChatMessageItem
import com.wenchen.yiyi.core.designSystem.component.BlinkingReplyIndicator
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.common.theme.BlackBg
import com.wenchen.yiyi.core.common.theme.HalfTransparentBlack
import com.wenchen.yiyi.core.common.theme.WhiteBg
import com.wenchen.yiyi.core.util.ThemeColorExtractor
import com.wenchen.yiyi.core.util.toast.ToastUtils
import com.wenchen.yiyi.feature.aiChat.viewmodel.SingleChatViewModel
import com.wenchen.yiyi.navigation.routes.AiChatRoutes
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.collections.map

@Composable
internal fun SingleChatRoute(
    viewModel: SingleChatViewModel = hiltViewModel(),
    navController: NavController
) {
    AIChatTheme {
        SingleChatScreen(viewModel, navController)
    }
}


@Composable
private fun SingleChatScreen(
    viewModel: SingleChatViewModel = hiltViewModel(),
    navController: NavController
) {
    SingleChatScreenContent(
        viewModel = viewModel,
        navController = navController
    )
}

@Composable
private fun SingleChatScreenContent(
    viewModel: SingleChatViewModel,
    navController: NavController
) {
    val activity = LocalActivity.current as ComponentActivity
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val bgImgHazeState = rememberHazeState()
    val chatWindowHazeState = rememberHazeState()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleImageResult(
            activity = activity,
            result = result,
            onImageSelected = { bitmap, uri ->
                viewModel.sendImage(bitmap, uri)
            },
            onError = { error ->
                // 处理错误
            }
        )
    }
    var bgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var colors by remember { mutableStateOf(listOf(BlackBg)) }
    // 异步加载背景图片
    LaunchedEffect(uiState.currentCharacter?.backgroundPath) {
        val backgroundPath = uiState.currentCharacter?.backgroundPath
        if (backgroundPath?.isNotEmpty() == true) {
            try {
                // 在后台线程执行图片加载
                viewModel.loadBackgroundBitmap(
                    activity = activity,
                    backgroundPath = backgroundPath,
                    onResult = { bitmap ->
                        bgBitmap = bitmap
                        colors = ThemeColorExtractor.extract(bgBitmap!!, maxValue = 1f)
                    }
                )
            } catch (e: Exception) {
                ToastUtils.showError("加载背景图片失败: ${e.message}")
                bgBitmap = null
            }
        } else {
            bgBitmap = null
        }
    }

    var showModelsDialog by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }
    // 抽屉使用RTL布局方向,此时抽屉从右向左滑动
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                NavigationDrawerContent(
                    viewModel = viewModel,
                    onNavSwitchModelClick = {
                        showModelsDialog = true
                    },
                    onEditClick = {
                        viewModel.navigate(AiChatRoutes.CharacterEdit(it.aiCharacterId))
                    },
                    onNavClearChatClick = {
                        showClearChatDialog = true
                    },
                    onDeleteClick = { showDeleteDialog = true },
                    onGotoConversationEdit = {
                        viewModel.navigate(
                            AiChatRoutes.ConversationEdit(
                                viewModel.conversation.value.id
                            )
                        )
                    },
                    onNavAboutClick = {
//                                scope.launch {
//                                    drawerState.close()
//                                }
                        // 获取聊天消息列表
                        val messages = viewModel.uiState.value.messages
                        // 提取所有消息内容
                        val contents = messages.map { it.content }
                        // 记录到日志
                        Timber.tag("Data").d("聊天消息列表: $contents")
                    },
                    themeBgColor = colors[0],
                    hazeState = bgImgHazeState,
                )
            },
            modifier = Modifier
                .background(BlackBg)
                .hazeSource(chatWindowHazeState),
        ) {
            // 确保聊天界面使用LTR布局方向
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AsyncImage(
                    model = bgBitmap,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .hazeSource(state = bgImgHazeState), // 标记需要被模糊的区域
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                HalfTransparentBlack.copy(0.65f),
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Transparent,
                                            ),
                                    ),
                            ),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(166.dp)
                                .align(Alignment.BottomCenter)
                                .hazeEffect(
                                    state = bgImgHazeState,
                                    style = HazeStyle(tint = null, blurRadius = 16.dp),
                                ) {
                                    // 垂直线性渐变：从顶部0%模糊强度到底部100%
                                    progressive =
                                        HazeProgressive.verticalGradient(
                                            startIntensity = 0f,
                                            endIntensity = 1f,
                                        )
                                }
                                .background(
                                    brush =
                                        Brush.verticalGradient(
                                            colors =
                                                listOf(
                                                    Color.Transparent,
                                                    colors[0].copy(0.6f),
                                                    colors[0].copy(0.8f),
                                                    colors[0],
                                                ),
                                        ),
                                ),
                    )
                    ChatScreen(
                        viewModel = viewModel,
                        onOpenDrawer = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onPickImage = {
                            val intent = viewModel.createImagePickerIntent()
                            launcher.launch(intent)
                        },
                        modifier = Modifier.fillMaxSize(),
                        themeBgColor = colors[0],
                        chatWindowHazeState = chatWindowHazeState
                    )
                }
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            if (showModelsDialog) {
                ModelsDialog(
                    supportedModels = viewModel.getSupportedModels(),
                    currentModel =
                        viewModel.uiState
                            .collectAsState()
                            .value.currentModelName,
                    onModelSelected = { modelId ->
                        viewModel.selectModel(modelId)
                        ToastUtils.showToast("已选择模型：$modelId")
                    },
                    onDismiss = { showModelsDialog = false },
                )
            }

            if (showClearChatDialog) {
                ClearChatDialog(
                    onConfirm = {
                        viewModel.clearChatHistory()
                        ToastUtils.showToast("聊天记录已清空")
                    },
                    onDismiss = { showClearChatDialog = false },
                )
            }

            if (showDeleteDialog) {
                DeleteCharacterDialog(
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        Timber.tag("SingleChatActivity")
                            .d("删除角色: ${uiState.currentCharacter?.name}")
                        viewModel.deleteCharacter(uiState.currentCharacter!!) {
                            showDeleteDialog = false
                        }
                    },
                    name = uiState.currentCharacter?.name ?: "[角色名称]",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: SingleChatViewModel,
    onOpenDrawer: () -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier,
    themeBgColor: Color = if (isSystemInDarkTheme()) BlackBg else WhiteBg,
    chatWindowHazeState: HazeState,
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var isSendSystemMessage by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

//    var contentLayoutHeight =
//        with(LocalDensity.current) { listState.layoutInfo.viewportSize.height.toDp() }
//    val lastItem =
//        remember { derivedStateOf { listState.layoutInfo } }.value.visibleItemsInfo.lastOrNull()
//    var lastItem2InputArea = if (lastItem != null) {
//        with(LocalDensity.current) { contentLayoutHeight - lastItem.offset.toDp() - lastItem.size.toDp() }.coerceAtLeast(
//            0.dp
//        )
//    } else {
//        0.dp
//    }

    // 滚动状态监听
    val scrollState = remember { mutableStateOf(false) } // 是否正在滚动
    val scrollEndState = remember { mutableStateOf(true) } // 滚动是否结束
    // 监听滚动状态
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                scrollState.value = isScrolling
                if (!isScrolling) {
                    // 延迟一段时间后标记滚动结束
                    delay(100) // 可调整延迟时间
                    scrollEndState.value = true
                } else {
                    scrollEndState.value = false
                }
            }
    }

    // 获取窗口Insets（用于判断键盘状态）
    val imeHeight =
        with(LocalDensity.current) {
            WindowInsets.ime.getBottom(this).toDp() // IME（键盘）的高度
        }
    // 监听键盘高度变化
    LaunchedEffect(imeHeight) {
        if (uiState.messages.isNotEmpty()) {
//            listState.scrollToItem(uiState.messages.size - 1)
            listState.scrollToItem(0)
        }
    }

    val initFinish = remember { mutableStateOf(false) }
    // 滚动到底部（仅在新消息时）
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty() && (uiState.receiveNewMessage || !initFinish.value)) {
//            listState.scrollToItem(uiState.messages.size - 1)
            listState.scrollToItem(0)
            initFinish.value = true
            if (uiState.receiveNewMessage) {
                viewModel.setReceiveNewMessage(false)
            }
        }
    }
    // 监听滚动位置以加载更多消息
    LaunchedEffect(listState, initFinish.value) {
        Timber.tag("ChatScreen").i("initFinish: ${initFinish.value}")
        if (!initFinish.value) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                // 当用户滚动到顶部附近时加载更多消息
                if (firstVisibleIndex < 2 && viewModel.canLoadMore()) {
                    viewModel.loadMoreMessages()
                }
            }
//        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull { it.index == uiState.messages.size - 1 } }
//            .collect { lastVisibleItem ->
//                if (lastVisibleItem != null && lastVisibleItem.index > 10) {
//                    if (viewModel.canLoadMore()) {
//                        viewModel.loadMoreMessages()
//                    }
//                }
//            }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            ChatActivityTopBar(onOpenDrawer, viewModel, scrollState.value, scrollEndState.value)
        },
        content = { padding ->
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
//                    .offset(y = -(imeHeight - padding.calculateBottomPadding()).coerceAtLeast(0.dp))
                        .padding(
                            start = padding.calculateLeftPadding(LayoutDirection.Ltr),
                            end = padding.calculateRightPadding(LayoutDirection.Ltr),
                            top = padding.calculateTopPadding(),
                        ),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .imePadding(),
//                        .offset(y = if (lastItem2InputArea > imeHeight) 0.dp else { -(imeHeight - padding.calculateBottomPadding()).coerceAtLeast(0.dp) + lastItem2InputArea })
                ) {
                    // 聊天消息列表
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = listState,
                        reverseLayout = true,
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            if (message.isShow) {
                                DisplayChatMessageItem(
                                    message = message,
                                    viewModel = viewModel,
                                    chatWindowHazeState = chatWindowHazeState
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (uiState.isAiReplying) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.Center,
                            content = { BlinkingReplyIndicator(text = "回复中...") }
                        )
                    }
                }
                // 输入区域
                ChatInputArea(
                    viewModel = viewModel,
                    modifier =
                        Modifier
                            .offset(
                                y =
                                    -(imeHeight - padding.calculateBottomPadding() + 3.dp).coerceAtLeast(
                                        0.dp,
                                    ),
                            )
                            .padding(bottom = padding.calculateBottomPadding()),
                    messageText = messageText,
                    onMessageTextChange = { messageText = it },
                    onSendMessage = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText, isSendSystemMessage)
                            messageText = ""
                        }
                    },
                    onPickImage = onPickImage,
                    onReGenerate = { viewModel.reGenerate(false) },
                    onContinue = { viewModel.continueGenerate(false) },
                    isAiReplying = uiState.isAiReplying,
                    isSendSystemMessage = isSendSystemMessage,
                    onSendSysMsgClick = { isSendSystemMessage = !isSendSystemMessage },
                    themeBgColor = themeBgColor,
                )
            }
        },
    )
}
