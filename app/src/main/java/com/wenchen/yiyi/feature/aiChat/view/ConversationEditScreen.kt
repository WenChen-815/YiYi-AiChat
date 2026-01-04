package com.wenchen.yiyi.feature.aiChat.view

import android.app.Activity
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.wenchen.yiyi.core.database.entity.AIChatMemory
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.designSystem.component.SettingTextFieldItem
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.common.theme.BlackText
import com.wenchen.yiyi.core.common.theme.DarkGray
import com.wenchen.yiyi.core.common.theme.GrayText
import com.wenchen.yiyi.core.common.theme.LightGold
import com.wenchen.yiyi.core.common.theme.LightGray
import com.wenchen.yiyi.core.common.theme.WhiteText
import com.wenchen.yiyi.core.util.StatusBarUtil
import com.wenchen.yiyi.feature.aiChat.viewmodel.ConversationEditViewModel
import com.wenchen.yiyi.feature.worldBook.model.WorldBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.compose.runtime.collectAsState

@Composable
internal fun ConversationEditRoute(
    viewModel: ConversationEditViewModel = hiltViewModel(),
    navController: NavController
) {
    AIChatTheme {
        ConversationEditScreen(viewModel, navController)
    }
}

@Composable
private fun ConversationEditScreen(
    viewModel: ConversationEditViewModel = hiltViewModel(),
    navController: NavController
) {
    val activity = LocalActivity.current as ComponentActivity
    val pickAvatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleImageResult(
            activity = activity,
            result = result,
            onImageSelected = { bitmap ->
                viewModel.avatarBitmap = bitmap
                viewModel.hasNewAvatar = true
            },
            onError = { error ->
                // 处理错误
            }
        )
    }
    val pickBackgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleImageResult(
            activity = activity,
            result = result,
            onImageSelected = { bitmap ->
                viewModel.backgroundBitmap = bitmap
                viewModel.hasNewBackground = true
            },
            onError = { error ->
                // 处理错误
            }
        )
    }

    ConversationEditScreenContent(
        viewModel = viewModel,
        activity = activity,
        isCreateNew = viewModel.isNewConversation.collectAsState().value,
        conversationId = viewModel.conversationId.collectAsState().value,
        onSaveClick = {
                name,
                playerName,
                playGender,
                playerDescription,
                chatWorldId,
                chatSceneDescription,
                additionalSummaryRequirement,
                chooseCharacterMap,
                characterKeywordsMap,
            ->
            viewModel.saveConversation(
                name,
                playerName,
                playGender,
                playerDescription,
                chatWorldId,
                chatSceneDescription,
                additionalSummaryRequirement,
                chooseCharacterMap,
                characterKeywordsMap,
            )
        },
        onCancelClick = { viewModel.navigateBack() },
        onAvatarClick = {
            val intent = viewModel.createImagePickerIntent()
            pickAvatarLauncher.launch(intent)
        },
        onBackgroundClick = {
            val intent = viewModel.createImagePickerIntent()
            pickBackgroundLauncher.launch(intent)
        },
        avatarBitmap = viewModel.avatarBitmap,
        backgroundBitmap = viewModel.backgroundBitmap,
        onAvatarDeleteClick = {
            viewModel.hasNewAvatar = false
            viewModel.avatarBitmap = null
        },
        onBackgroundDeleteClick = {
            viewModel.hasNewBackground = false
            viewModel.backgroundBitmap = null
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationEditScreenContent(
    viewModel: ConversationEditViewModel,
    activity: Activity,
    isCreateNew: Boolean,
    conversationId: String,
    onSaveClick: (String, String, String, String, String, String, String, Map<String, Float>, Map<String, List<String>>) -> Unit,
    onCancelClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onBackgroundClick: () -> Unit,
    avatarBitmap: Bitmap?,
    backgroundBitmap: Bitmap?,
    onAvatarDeleteClick: () -> Unit,
    onBackgroundDeleteClick: () -> Unit,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    if (isSystemInDarkTheme()) {
        StatusBarUtil.setStatusBarTextColor(activity, false)
    } else {
        StatusBarUtil.setStatusBarTextColor(activity, true)
    }
    val userConfig by viewModel.userConfigState.userConfig.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var playerName by remember { mutableStateOf(userConfig?.userName ?: "") }
    var playGender by remember { mutableStateOf("") }
    var playerDescription by remember { mutableStateOf("") }
    var chatWorldId by remember { mutableStateOf("") }
    var chatSceneDescription by remember { mutableStateOf("") }
    var additionalSummaryRequirement by remember { mutableStateOf("") }
    var chooseCharacterMap by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var characterKeywordsMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var allCharacter by remember { mutableStateOf<List<AICharacter>>(emptyList()) }
    var allWorldBook by remember { mutableStateOf<List<WorldBook>>(emptyList()) }
    val moshi: Moshi = Moshi.Builder().build()
    val worldBookAdapter: JsonAdapter<WorldBook> = moshi.adapter(WorldBook::class.java)

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val conversationDao = Application.appDatabase.conversationDao()
    val aiCharacterDao = Application.appDatabase.aiCharacterDao()
    var avatarPath by remember { mutableStateOf("") }
    var backgroundPath by remember { mutableStateOf("") }
    var conversation by remember { mutableStateOf<Conversation?>(null) }

    val (isCharacterSheetVisible, setCharacterSheetVisible) = remember { mutableStateOf(false) }
    val (isWorldSheetVisible, setWorldSheetVisible) = remember { mutableStateOf(false) }
    val onCharacterSelectedCallback = remember { mutableStateOf<((String) -> Unit)?>(null) }

    BackHandler(enabled = isCharacterSheetVisible) {
        setCharacterSheetVisible(false)
    }
    LaunchedEffect(conversationId) {
        launch(Dispatchers.IO) {
            try {
                allCharacter = aiCharacterDao.getAllCharacters()
                conversation = conversationDao.getById(conversationId)
                if (conversation != null) {
                    name = conversation!!.name
                    playerName = conversation!!.playerName
                    playGender = conversation!!.playGender
                    playerDescription = conversation!!.playerDescription
                    chatWorldId = conversation!!.chatWorldId
                    chatSceneDescription = conversation!!.chatSceneDescription
                    additionalSummaryRequirement = conversation!!.additionalSummaryRequirement ?: ""
                    avatarPath = conversation!!.avatarPath.toString()
                    backgroundPath = conversation!!.backgroundPath.toString()
                    chooseCharacterMap = conversation!!.characterIds
                    characterKeywordsMap = conversation?.characterKeywords ?: emptyMap()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast
                        .makeText(context, "加载对话数据失败: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadWorldBooks(worldBookAdapter) { loadedBooks ->
                        allWorldBook = loadedBooks
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "返回",
                    modifier =
                        Modifier
                            .clickable { viewModel.navigateBack() }
                            .size(18.dp)
                            .align(Alignment.CenterStart),
                )
                Text(
                    text = "对话配置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        },
        bottomBar = {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                        ),
                ) {
                    Text("取消编辑", color = WhiteText)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        onSaveClick(
                            name,
                            playerName,
                            playGender,
                            playerDescription,
                            chatWorldId,
                            chatSceneDescription,
                            additionalSummaryRequirement,
                            chooseCharacterMap,
                            characterKeywordsMap,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Text("保存对话", color = WhiteText)
                }
            }
        },
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding(),
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
        ) {
            if (isCreateNew || conversation?.type == ConversationType.GROUP) {
                SettingTextFieldItem(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    singleLine = true,
                    label = "对话名称",
                    labelPadding = PaddingValues(bottom = 6.dp),
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("请输入对话名称") },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "参与角色",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "（右划管理角色）",
                            style = MaterialTheme.typography.labelMedium.copy(GrayText),
                        )
                    }

                    TextButton(
                        onClick = {
                            onCharacterSelectedCallback.value = { characterId ->
                                chooseCharacterMap = chooseCharacterMap + (characterId to 0.8f)
                                characterKeywordsMap =
                                    characterKeywordsMap + (characterId to emptyList())
                            }
                            setCharacterSheetVisible(true)
                        },
                    ) {
                        Text("添加角色", color = MaterialTheme.colorScheme.secondary)
                    }
                }

                var isMemoryDialogVisible by remember { mutableStateOf(false) }
                var selectCharacterId by remember { mutableStateOf("") }
                chooseCharacterMap.forEach { (characterId, chance) ->
                    var offsetX by remember { mutableFloatStateOf(0f) }
                    val maxOffset = with(LocalDensity.current) { (56.dp * 2).toPx() } // 最大偏移量
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // 在偏移区域显示操作按钮
                        Row(
                            modifier = Modifier
                                .height(72.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .zIndex(if (offsetX == maxOffset) 1f else 0f)
                        ) {
                            // 角色记忆按钮
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(56.dp)
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .padding(horizontal = 6.dp)
                                    .clickable {
                                        selectCharacterId = characterId
                                        isMemoryDialogVisible = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "查看\n记忆",
                                    color = WhiteText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // 删除角色按钮
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(56.dp)
                                    .background(Color.Red)
                                    .padding(horizontal = 6.dp)
                                    .clickable {
                                        chooseCharacterMap = chooseCharacterMap - characterId
                                        // 同步移除关键词表中的对应角色关键词
                                        characterKeywordsMap = characterKeywordsMap - characterId
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "删除\n角色",
                                    color = WhiteText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Column {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .padding(vertical = 4.dp)
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                // 拖拽结束后，根据偏移量决定最终位置
                                                offsetX = when {
                                                    offsetX > maxOffset / 2 -> maxOffset // 右滑超过一半，完全展开
                                                    offsetX < -maxOffset / 2 -> 0f       // 左滑超过一半，回到原位
                                                    else -> if (offsetX > 0) 0f else offsetX // 其他情况回到原位
                                                }
                                            }
                                        ) { change, dragAmount ->
                                            // 允许左右拖拽，但限制最大偏移量
                                            val newValue = offsetX + dragAmount
                                            if (newValue >= 0 && newValue <= maxOffset) {
                                                offsetX = newValue
                                                change.consume()
                                            }
                                        }
                                    }
                                    .offset { IntOffset(offsetX.toInt(), 0) },
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = allCharacter.find { it.aiCharacterId == characterId }?.name
                                            ?: characterId,
                                        style = MaterialTheme.typography.bodyMedium.copy(WhiteText),
                                        modifier = Modifier.weight(1f),
                                    )

                                    // 概率调整滑块
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val colors =
                                            SliderDefaults.colors(
                                                activeTrackColor = WhiteText,
                                                inactiveTrackColor = LightGold,
                                                thumbColor = WhiteText,
                                                activeTickColor = Color.Transparent,
                                                inactiveTickColor = Color.White,
                                            )
                                        Slider(
                                            value = chance,
                                            onValueChange = { newChance ->
                                                chooseCharacterMap =
                                                    chooseCharacterMap + (characterId to newChance)
                                            },
                                            modifier = Modifier.width(120.dp),
                                            colors = colors,
                                            steps = 19,
                                            thumb = {
                                                Canvas(modifier = Modifier.size(24.dp)) {
                                                    drawCircle(
                                                        color = WhiteText,
                                                        radius = 7.dp.toPx(),
                                                    )
                                                }
                                            },
                                            track = { sliderState ->
                                                SliderDefaults.Track(
                                                    modifier = Modifier.height(9.dp),
                                                    colors = colors,
                                                    enabled = true,
                                                    sliderState = sliderState,
                                                    thumbTrackGapSize = 0.dp,
                                                    drawStopIndicator = {
//                                                drawCircle(color = colors.inactiveTickColor, center = it, radius = TrackStopIndicatorSize.toPx() / 2f)
                                                    },
                                                    // 求求了，别画那些小圆点，真不好看
                                                    drawTick = { offset, color -> {} },
                                                )
                                            },
                                            valueRange = 0.0f..1.0f,
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(LightGold.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${(chance * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    WhiteText
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                            val (keywordsText, setKeywordsText) = remember(characterId) {
                                mutableStateOf(
                                    characterKeywordsMap[characterId]?.joinToString("/") ?: ""
                                )
                            }
                            // 关键词输入框
                            SettingTextFieldItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                                singleLine = true,
                                label = "",
                                value = keywordsText,
                                onValueChange = { newText ->
                                    setKeywordsText(newText)
                                    // 更新关键词映射
                                    val keywordsList = if (newText.isBlank()) {
                                        emptyList()
                                    } else {
                                        newText.split("/").map { it.trim() }
                                            .filter { it.isNotEmpty() }
                                    }
                                    characterKeywordsMap =
                                        characterKeywordsMap + (characterId to keywordsList)
                                },
                                placeholder = { Text("输入关键词，用/分隔", color = GrayText) },
                            )
                        }
                        if (isMemoryDialogVisible) {
                            val coroutineScope = rememberCoroutineScope()
                            ShowMemoryDialog(
                                onConfirm = { newMemoryContent, memoryCount ->
                                    // 启动新线程保存角色记忆
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val memoryDao =
                                                Application.appDatabase.aiChatMemoryDao()
                                            // 检查是否已存在该角色的记忆
                                            val existingMemory =
                                                memoryDao.getByCharacterIdAndConversationId(
                                                    selectCharacterId,
                                                    conversationId
                                                )

                                            if (existingMemory != null) {
                                                // 更新现有记忆
                                                val updatedMemory = existingMemory.copy(
                                                    content = newMemoryContent,
                                                    count = memoryCount
                                                )
                                                memoryDao.update(updatedMemory)
                                            } else {
                                                // 创建新的记忆记录
                                                val newMemory = AIChatMemory(
                                                    id = UUID.randomUUID().toString(),
                                                    characterId = selectCharacterId,
                                                    conversationId = conversationId,
                                                    content = newMemoryContent,
                                                    createdAt = System.currentTimeMillis(),
                                                    count = memoryCount
                                                )
                                                memoryDao.insert(newMemory)
                                            }

                                            // 更新本地状态
                                            withContext(Dispatchers.Main) {
                                                isMemoryDialogVisible = false
                                                Toast.makeText(
                                                    context,
                                                    "角色记忆保存成功",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "保存失败: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                onDismiss = { isMemoryDialogVisible = false },
                                characterId = selectCharacterId,
                                conversationId = conversationId,
                            )
                        }
                    }
                }

                Text(
                    text = "群聊头像",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                Box(
                    modifier =
                        Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSystemInDarkTheme()) DarkGray else LightGray)
                            .clickable { onAvatarClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = "群聊头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        // 删除头像按钮
                        IconButton(
                            onClick = { onAvatarDeleteClick() },
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .offset(8.dp, (-8).dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RemoveCircleOutline,
                                contentDescription = "删除头像",
                                tint = BlackText,
                            )
                        }
                    } else if (avatarPath.isNotEmpty()) {
                        // 使用 Coil 加载已有头像
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalContext.current)
                                    .data(avatarPath.toUri())
                                    .crossfade(true)
                                    .build(),
                            contentDescription = "角色头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        // 显示默认头像或添加图标
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加头像",
                            )
                            Text("添加头像", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 聊天背景
                Text(
                    text = "聊天背景",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSystemInDarkTheme()) DarkGray else LightGray)
                            .clickable { onBackgroundClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (backgroundBitmap != null) {
                        Image(
                            bitmap = backgroundBitmap.asImageBitmap(),
                            contentDescription = "聊天背景",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        // 删除背景按钮
                        IconButton(
                            onClick = { onBackgroundDeleteClick() },
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .offset(8.dp, (-8).dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RemoveCircleOutline,
                                contentDescription = "删除背景",
                                tint = BlackText,
                            )
                        }
                    } else if (backgroundPath.isNotEmpty()) {
                        // 使用 Coil 加载已有背景
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalContext.current)
                                    .data(backgroundPath.toUri())
                                    .crossfade(true)
                                    .build(),
                            contentDescription = "聊天背景",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        // 显示默认背景或添加图标
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加背景",
                            )
                            Text("添加背景", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            SettingTextFieldItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                singleLine = true,
                label = "我的名称",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = playerName,
                onValueChange = { playerName = it },
                placeholder = { Text("请输入名称") },
            )

            SettingTextFieldItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                singleLine = true,
                label = "我的性别",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = playGender,
                onValueChange = { playGender = it },
                placeholder = { Text("请输入性别，如沃尔玛购物袋") },
            )

            SettingTextFieldItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 250.dp),
                label = "我的描述",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = playerDescription,
                onValueChange = { playerDescription = it },
                placeholder = { Text("请输入对自己的描述") },
                minLines = 3,
                maxLines = 10,
            )

//            SettingTextFieldItem(
//                modifier =
//                    Modifier
//                        .fillMaxWidth()
//                        .padding(top = 8.dp),
//                singleLine = true,
//                label = "世界ID",
//                labelPadding = PaddingValues(bottom = 6.dp),
//                value = chatWorldId,
//                onValueChange = { chatWorldId = it },
//                placeholder = { Text("未实现，敬请期待") },
//            )
            Text(
                text = "世界选择",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { setWorldSheetVisible(true) }
                        .padding(16.dp)) {
                    Text(
                        text = allWorldBook.find { it.id == chatWorldId }?.worldName
                            ?: "未选择世界",
                        modifier = Modifier.fillMaxWidth(),
                        color = WhiteText
                    )
                }
            }

            SettingTextFieldItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 250.dp),
                label = "场景描述",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = chatSceneDescription,
                onValueChange = { chatSceneDescription = it },
                placeholder = { Text("请输入场景描述") },
                minLines = 5,
                maxLines = 15,
            )
            SettingTextFieldItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 250.dp),
                label = "额外总结需求",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = additionalSummaryRequirement,
                onValueChange = { additionalSummaryRequirement = it },
                placeholder = { Text("非必填：AI总结时会关注这里的额外要求，尽可能保留你希望的内容") },
                minLines = 3,
                maxLines = 10,
            )
            if (isCharacterSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = {
                        setCharacterSheetVisible(false)
                    },
                    sheetState =
                        rememberModalBottomSheetState(
                            skipPartiallyExpanded = true, // 禁用部分展开
                        ),
                ) {
                    // 获取屏幕高度
                    val screenHeight =
                        with(LocalDensity.current) {
                            LocalWindowInfo.current.containerSize.height
                                .toDp()
                        }
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = screenHeight * 0.6f),
                    ) {
                        items(allCharacter.size) { index ->
                            val character = allCharacter[index]
                            val id = character.aiCharacterId
                            val name = character.name
                            ListItem(
                                headlineContent = { Text(name) },
                                modifier =
                                    Modifier.clickable {
                                        if (!chooseCharacterMap.contains(id)) {
                                            onCharacterSelectedCallback.value?.invoke(id)
                                        }
                                    },
                            )
                        }
                    }
                }
            }
            if (isWorldSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = {
                        setWorldSheetVisible(false)
                    },
                    sheetState = rememberModalBottomSheetState(
                        skipPartiallyExpanded = true // 禁用部分展开
                    ),
                ) {
                    // 获取屏幕高度
                    val screenHeight =
                        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = screenHeight * 0.6f)
                    ) {
                        items(allWorldBook.size) { index ->
                            val worldBook = allWorldBook[index]
                            ListItem(
                                headlineContent = { Text(worldBook.worldName) },
                                modifier = Modifier.clickable {
                                    chatWorldId = worldBook.id
                                    setWorldSheetVisible(false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
