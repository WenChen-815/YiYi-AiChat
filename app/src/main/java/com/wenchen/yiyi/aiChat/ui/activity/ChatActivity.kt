package com.wenchen.yiyi.aiChat.ui.activity

import android.R.attr.padding
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.wenchen.yiyi.aiChat.entity.ChatMessage
import com.wenchen.yiyi.aiChat.vm.ChatViewModel
import com.wenchen.yiyi.aiChat.common.AIChatManager
import com.wenchen.yiyi.aiChat.common.ImageManager
import com.wenchen.yiyi.aiChat.ui.ChatActivityTopBar
import com.wenchen.yiyi.aiChat.ui.ChatInputArea
import com.wenchen.yiyi.aiChat.ui.ClearChatDialog
import com.wenchen.yiyi.aiChat.ui.DeleteCharacterDialog
import com.wenchen.yiyi.common.theme.AIChatTheme
import com.wenchen.yiyi.common.utils.StatusBarUtil
import com.wenchen.yiyi.config.ui.activity.ConfigActivity
import kotlin.collections.map
import com.wenchen.yiyi.aiChat.ui.DisplayChatMessageItem
import com.wenchen.yiyi.aiChat.ui.ModelsDialog
import com.wenchen.yiyi.aiChat.ui.NavigationDrawerContent
import com.wenchen.yiyi.common.theme.BlackBg
import com.wenchen.yiyi.common.theme.HalfTransparentBlack
import com.wenchen.yiyi.common.theme.WhiteBg
import com.wenchen.yiyi.common.utils.ThemeColorExtractor
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlin.jvm.java

class ChatActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                            contentResolver,
                            imageUri!!
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                }                // 保存图片并发送
                val file = saveImageToCache(bitmap)
                if (file != null) {
                    val savedUri = Uri.fromFile(file)
                    viewModel.sendImage(bitmap, savedUri)
                    Toast.makeText(this, "图片选择成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        StatusBarUtil.transparentNavBar(this)
        StatusBarUtil.setStatusBarTextColor(this, false)
        super.onCreate(savedInstanceState)

        // 初始化聊天监听器
        setupChatListener()

        // 获取角色信息
        val characterJson = intent.getStringExtra("SELECTED_CHARACTER")
        val characterId = intent.getStringExtra("SELECTED_CHARACTER_ID")
        viewModel.initChat(characterJson, characterId)

        setContent {
            AIChatTheme {
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val uiState by viewModel.uiState.collectAsState()
                val bgImgHazeState = rememberHazeState()

                val bgBitmap: Bitmap? = try {
                    if (uiState.currentCharacter?.backgroundPath?.isNotEmpty() == true) {
                        val file = File(uiState.currentCharacter?.backgroundPath!!)
                        if (file.exists()) {
                            val uri = Uri.fromFile(file)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val bitmap = ImageDecoder.decodeBitmap(
                                    ImageDecoder.createSource(contentResolver, uri)
                                ) { decoder, info, source ->
                                    // 禁用硬件加速，确保可以访问像素
                                    decoder.setTargetColorSpace(
                                        ColorSpace.get(
                                            ColorSpace.Named.SRGB
                                        )
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
                                MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            }
                        } else {
                            Log.e(
                                "ChatActivity",
                                "背景图片文件不存在: ${uiState.currentCharacter?.backgroundPath}"
                            )
                            null
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("ChatActivity", "加载背景图片失败: ${e.message}", e)
                    null
                }
                val colors: List<Color> = if (bgBitmap != null) {
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
                                onNavConfigClick = {
                                    startActivity(
                                        Intent(
                                            this@ChatActivity,
                                            ConfigActivity::class.java
                                        )
                                    )
                                },
                                onNavSwitchModelClick = {
                                    showModelsDialog = true
                                },
                                onEditClick = {
                                    val intent =
                                        Intent(this@ChatActivity, CharacterEditActivity::class.java)
                                    intent.putExtra("CHARACTER_ID", it.aiCharacterId)
                                    this@ChatActivity.startActivity(intent)
                                },
                                onNavClearChatClick = {
                                    showClearChatDialog = true
                                },
                                onDeleteClick = { showDeleteDialog = true },
                                onGotoConversationEdit = {
                                    val intent = Intent(
                                        this@ChatActivity,
                                        ConversationEditActivity::class.java
                                    )
                                    intent.putExtra("CONVERSATION_ID", uiState.conversation.id)
                                    this@ChatActivity.startActivity(intent)
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
                                    Log.d("Data", "聊天消息列表: $contents")
                                },
                                themeBgColor = colors[0],
                                hazeState = bgImgHazeState,
                            )
                        },
                        modifier = Modifier.background(BlackBg)
                    ) {
                        // 确保聊天界面使用LTR布局方向
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            AsyncImage(
                                model = uiState.currentCharacter?.backgroundPath,
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .hazeSource(state = bgImgHazeState) // 标记需要被模糊的区域
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                HalfTransparentBlack.copy(0.65f),
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Transparent,
                                            )
                                        )
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(166.dp)
                                        .align(Alignment.BottomCenter)
                                        .hazeEffect(
                                            state = bgImgHazeState,
                                            style = HazeStyle(tint = null, blurRadius = 111.dp)
                                        ) {
                                            // 垂直线性渐变：从顶部0%模糊强度到底部100%
                                            progressive = HazeProgressive.verticalGradient(
                                                startIntensity = 0f,
                                                endIntensity = 1f
                                            )
                                        }
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    colors[0].copy(0.6f),
                                                    colors[0].copy(0.8f),
                                                    colors[0]
                                                )
                                            )
                                        )
                                )
                                ChatScreen(
                                    viewModel = viewModel,
                                    onOpenDrawer = {
                                        scope.launch {
                                            drawerState.open()
                                        }
                                    },
                                    onPickImage = {
                                        pickImageFromGallery()
                                    },
                                    onSendMessage = { message, isSendSystemMessage ->
                                        viewModel.sendMessage(message, isSendSystemMessage)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    themeBgColor = colors[0],
                                )
                            }

                        }
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        if (showModelsDialog) {
                            ModelsDialog(
                                supportedModels = viewModel.getSupportedModels(),
                                currentModel = viewModel.uiState.collectAsState().value.currentModelName,
                                onModelSelected = { modelId ->
                                    viewModel.selectModel(modelId)
                                    Toast.makeText(
                                        this@ChatActivity,
                                        "已选择模型：$modelId",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onDismiss = { showModelsDialog = false }
                            )
                        }

                        if (showClearChatDialog) {
                            ClearChatDialog(
                                onConfirm = {
                                    viewModel.clearChatHistory()
                                    Toast.makeText(
                                        this@ChatActivity,
                                        "聊天记录已清空",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onDismiss = { showClearChatDialog = false }
                            )
                        }

                        if (showDeleteDialog) {
                            DeleteCharacterDialog(
                                onDismiss = { showDeleteDialog = false },
                                onConfirm = {
                                    Log.d(
                                        "ChatActivity",
                                        "删除角色: ${uiState.currentCharacter?.name}"
                                    )
                                    // 使用协程调用挂起函数
                                    viewModel.viewModelScope.launch {
                                        if (viewModel.deleteCharacter(uiState.currentCharacter!!)) {
                                            Toast.makeText(
                                                this@ChatActivity,
                                                "角色删除成功",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            this@ChatActivity.finish()
                                        } else {
                                            Toast.makeText(
                                                this@ChatActivity,
                                                "角色删除失败",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        showDeleteDialog = false
                                    }
                                },
                                name = uiState.currentCharacter?.name ?: "[角色名称]"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupChatListener() {
        val listener = object : AIChatManager.AIChatMessageListener {
            override fun onMessageSent(userMessage: ChatMessage) {
                viewModel.addMessage(userMessage)
            }

            override fun onMessageReceived(aiMessage: ChatMessage) {
                viewModel.addMessage(aiMessage)
                // 隐藏进度条
                viewModel.hideProgress()
            }

            override fun onError(errorMessage: String) {
                runOnUiThread {
                    Log.e("ChatActivity", errorMessage)
                    Toast.makeText(this@ChatActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            override fun onShowToast(message: String) {
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        AIChatManager.registerListener(listener)
    }

    private fun pickImageFromGallery() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
            }
        } else {
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        pickImageLauncher.launch(intent)
    }

    private fun saveImageToCache(bitmap: Bitmap): File? {
        return try {
            val imageManager = ImageManager()
            val conversationId = "${
                viewModel.getConfigManager().getUserId().toString()
            }_${viewModel.getCurrentCharacter()?.aiCharacterId}"
            imageManager.saveChatImage(conversationId, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onResume() {
        super.onResume()
        // 刷新角色信息
        viewModel.refreshCharacterInfo()
        // 刷新对话信息
        viewModel.refreshConversationInfo()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenDrawer: () -> Unit,
    onPickImage: () -> Unit,
    onSendMessage: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    themeBgColor: Color = if (isSystemInDarkTheme()) BlackBg else WhiteBg,
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var isSendSystemMessage by remember { mutableStateOf(false) }

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
        Log.i("ChatScreen", "listState: $listState")
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
    val imeHeight = with(LocalDensity.current) {
        WindowInsets.ime.getBottom(this).toDp() // IME（键盘）的高度
    }
    // 监听键盘高度变化
    LaunchedEffect(imeHeight) {
        if (uiState.messages.isNotEmpty()) {
//            listState.scrollToItem(uiState.messages.size - 1)
            listState.scrollToItem(0)
        }
    }

    var initFinish = remember { mutableStateOf(false) }
    // 滚动到底部（仅在新消息时）
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty() && (uiState.receiveNewMessage || !initFinish.value)) {
//            listState.scrollToItem(uiState.messages.size - 1)
            listState.scrollToItem(0)
            initFinish.value = true
        }
    }
    // 监听滚动位置以加载更多消息
    LaunchedEffect(listState, initFinish.value) {
        Log.i("ChatScreen", "initFinish: ${initFinish.value}")
        if (!initFinish.value) return@LaunchedEffect
//        snapshotFlow { listState.firstVisibleItemIndex }
//            .collect { firstVisibleIndex ->
//                // 当用户滚动到顶部附近时加载更多消息
//                if (firstVisibleIndex < 2 && viewModel.canLoadMore()) {
//                    viewModel.loadMoreMessages()
//                }
//            }
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull { it.index == uiState.messages.size - 1 } }
            .collect { lastVisibleItem ->
                if (lastVisibleItem != null && lastVisibleItem.index > 10) {
                    if (viewModel.canLoadMore()) {
                        viewModel.loadMoreMessages()
                    }
                }
            }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            ChatActivityTopBar(onOpenDrawer, uiState,scrollState.value,scrollEndState.value)
        },
        content = { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
//                    .offset(y = -(imeHeight - padding.calculateBottomPadding()).coerceAtLeast(0.dp))
                    .padding(
                        start = padding.calculateLeftPadding(LayoutDirection.Ltr),
                        end = padding.calculateRightPadding(LayoutDirection.Ltr),
                        top = padding.calculateTopPadding(),
                    )
            ) {
                Box(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp).imePadding(),
//                        .offset(y = if (lastItem2InputArea > imeHeight) 0.dp else { -(imeHeight - padding.calculateBottomPadding()).coerceAtLeast(0.dp) + lastItem2InputArea })
                ){
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
                                )
                            }
                        }
                        item{
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
                // 输入区域
                ChatInputArea(
                    viewModel = viewModel,
                    modifier = Modifier
                        .offset(
                            y = -(imeHeight - padding.calculateBottomPadding() + 3.dp).coerceAtLeast(
                                0.dp
                            )
                        )
                        .padding(bottom = padding.calculateBottomPadding()),
                    messageText = messageText,
                    onMessageTextChange = { messageText = it },
                    onSendMessage = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText, isSendSystemMessage)
                            messageText = ""
                        }
                    },
                    onPickImage = onPickImage,
                    isAiReplying = uiState.isAiReplying,
                    isSendSystemMessage = isSendSystemMessage,
                    onSendSysMsgClick = { isSendSystemMessage = !isSendSystemMessage },
                    themeBgColor = themeBgColor,
                )
            }
        }
    )
}