package com.wenchen.yiyi.feature.aiChat.view

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.wenchen.yiyi.feature.aiChat.component.DisplayChatMessageItem
import com.wenchen.yiyi.core.common.components.BlinkingReplyIndicator
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.common.theme.BlackBg
import com.wenchen.yiyi.core.common.theme.HalfTransparentBlack
import com.wenchen.yiyi.core.common.theme.WhiteBg
import com.wenchen.yiyi.core.common.utils.ThemeColorExtractor
import com.wenchen.yiyi.core.util.toast.ToastUtils
import com.wenchen.yiyi.feature.aiChat.vm.GroupChatViewModel
import com.wenchen.yiyi.navigation.routes.AiChatRoutes
import dev.chrisbanes.haze.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

@Composable
internal fun GroupChatRoute (
    viewModel: GroupChatViewModel = hiltViewModel(),
    navController: NavController
){
    AIChatTheme{
        GroupChatScreen(viewModel, navController)
    }
}
@Composable
private fun GroupChatScreen(
    viewModel: GroupChatViewModel = hiltViewModel(),
    navController: NavController
){
    GroupChatScreenContent(
        viewModel = viewModel,
        navController = navController
    )
}
    @SuppressLint("ContextCastToActivity")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GroupChatScreenContent(
        viewModel: GroupChatViewModel,
        navController: NavController
    ) {
        val activity = LocalContext.current as ComponentActivity
        val conversation by viewModel.conversation.collectAsState()
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

        val bgBitmap: Bitmap? =
            try {
                if (conversation.backgroundPath?.isNotEmpty() == true) {
                    val file = File(conversation.backgroundPath)
                    if (file.exists()) {
                        val uri = Uri.fromFile(file)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val bitmap =
                                ImageDecoder.decodeBitmap(
                                    ImageDecoder.createSource(activity.contentResolver, uri),
                                ) { decoder, info, source ->
                                    // 禁用硬件加速，确保可以访问像素
                                    decoder.setTargetColorSpace(
                                        ColorSpace.get(
                                            ColorSpace.Named.SRGB,
                                        ),
                                    )
                                }
                            // 如果是硬件Bitmap，转换为可修改的Bitmap
                            if (bitmap.config == Bitmap.Config.HARDWARE) {
                                bitmap.copy(Bitmap.Config.ARGB_8888, false)
                            } else {
                                bitmap
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
                        }
                    } else {
                        Log.e(
                            "GroupChatActivity",
                            "背景图片文件不存在: ${conversation.backgroundPath}",
                        )
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("GroupChatActivity", "加载背景图片失败: ${e.message}", e)
                null
            }

        val colors: List<Color> =
            if (bgBitmap != null) {
                ThemeColorExtractor().extract(bgBitmap, maxValue = 1f)
            } else {
                listOf(BlackBg)
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
                        onNavClearChatClick = {
                            showClearChatDialog = true
                        },
                        onDeleteGroupClick = { showDeleteDialog = true },
                        onGotoConversationEdit = {
                            viewModel.navigate(AiChatRoutes.ConversationEdit(
                                viewModel.conversation.value.id
                            ))
                        },
                        onNavAboutClick = {
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
                modifier = Modifier.background(BlackBg).hazeSource(chatWindowHazeState),
            ) {
                // 确保聊天界面使用LTR布局方向
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    AsyncImage(
                        model = conversation.backgroundPath,
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
                                        style = HazeStyle(tint = null, blurRadius = 32.dp),
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
                        GroupChatContent(
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
                            chatWindowHazeState = chatWindowHazeState,
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
                            ToastUtils.show("已选择模型：$modelId")
                        },
                        onDismiss = { showModelsDialog = false },
                    )
                }

                if (showClearChatDialog) {
                    ClearChatDialog(
                        onConfirm = {
                            viewModel.clearChatHistory()
                            ToastUtils.show("聊天记录已清空")
                        },
                        onDismiss = { showClearChatDialog = false },
                    )
                }

                if (showDeleteDialog) {
                    DeleteCharacterDialog(
                        onDismiss = { showDeleteDialog = false },
                        onConfirm = {
                            Log.d(
                                "GroupChatActivity",
                                "删除对话: ${conversation.name}",
                            )
                            // 使用协程调用挂起函数
                            viewModel.viewModelScope.launch {
                                if (viewModel.deleteConversation(conversation)) {
                                    ToastUtils.show("对话已删除")
                                } else {
                                    ToastUtils.show("删除对话失败")
                                }
                                showDeleteDialog = false
                            }
                        },
                        name = conversation.name,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    fun GroupChatContent(
        viewModel: GroupChatViewModel,
        onOpenDrawer: () -> Unit,
        onPickImage: () -> Unit,
        modifier: Modifier = Modifier,
        themeBgColor: Color = if (isSystemInDarkTheme()) BlackBg else WhiteBg,
        chatWindowHazeState: HazeState
    ) {
        val uiState by viewModel.uiState.collectAsState()
        var messageText by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        var isSendSystemMessage by remember { mutableStateOf(false) }

        // 滚动状态监听
        val scrollState = remember { mutableStateOf(false) } // 是否正在滚动
        val scrollEndState = remember { mutableStateOf(true) } // 滚动是否结束
        // 监听滚动状态
        LaunchedEffect(listState) {
            Log.i("GroupChatContent", "listState: $listState")
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
                listState.scrollToItem(0)
            }
        }

        val initFinish = remember { mutableStateOf(false) }
        // 滚动到底部（仅在新消息时）
        LaunchedEffect(uiState.messages.size) {
            if (uiState.messages.isNotEmpty() && (uiState.receiveNewMessage || !initFinish.value)) {
                listState.scrollToItem(0)
                initFinish.value = true
            }
        }
        // 监听滚动位置以加载更多消息
        LaunchedEffect(listState, initFinish.value) {
            Log.i("ChatScreen", "initFinish: ${initFinish.value}")
            if (!initFinish.value) return@LaunchedEffect
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { firstVisibleIndex ->
                    // 当用户滚动到顶部附近时加载更多消息
                    if (firstVisibleIndex < 2 && viewModel.canLoadMore()) {
                        viewModel.loadMoreMessages()
                    }
                }
//            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull { it.index == uiState.messages.size - 1 } }
//                .collect { lastVisibleItem ->
//                    if (lastVisibleItem != null && lastVisibleItem.index > 10) {
//                        if (viewModel.canLoadMore()) {
//                            viewModel.loadMoreMessages()
//                        }
//                    }
//                }
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
                    ) {
                        // 聊天消息列表
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            state = listState,
                            reverseLayout = true,
                        ) {
                            items(uiState.messages) { message ->
                                if (message.isShow) {
                                    DisplayChatMessageItem(
                                        message = message,
                                        viewModel = viewModel,
                                        chatWindowHazeState = chatWindowHazeState,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (uiState.isAiReplying) {
                            Row(
                                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
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
                                viewModel.sendGroupMessage(messageText, isSendSystemMessage)
                                messageText = ""
                            }
                        },
                        onPickImage = onPickImage,
                        onReGenerate = { viewModel.reGenerate(true) },
                        onContinue = { viewModel.continueGenerate(true) },
                        isAiReplying = uiState.isAiReplying,
                        isSendSystemMessage = isSendSystemMessage,
                        onSendSysMsgClick = { isSendSystemMessage = !isSendSystemMessage },
                        themeBgColor = themeBgColor,
                    )
                }
            },
        )
    }
