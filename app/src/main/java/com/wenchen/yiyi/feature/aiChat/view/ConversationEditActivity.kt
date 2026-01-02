package com.wenchen.yiyi.feature.aiChat.view

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.wenchen.yiyi.feature.aiChat.common.ImageManager
import com.wenchen.yiyi.feature.aiChat.entity.AIChatMemory
import com.wenchen.yiyi.feature.aiChat.entity.Conversation
import com.wenchen.yiyi.feature.aiChat.entity.ConversationType
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.common.components.SettingTextFieldItem
import com.wenchen.yiyi.core.common.entity.AICharacter
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.common.theme.BlackText
import com.wenchen.yiyi.core.common.theme.DarkGray
import com.wenchen.yiyi.core.common.theme.GrayText
import com.wenchen.yiyi.core.common.theme.LightGold
import com.wenchen.yiyi.core.common.theme.LightGray
import com.wenchen.yiyi.core.common.theme.WhiteText
import com.wenchen.yiyi.core.common.utils.FilesUtil
import com.wenchen.yiyi.core.common.utils.StatusBarUtil
import com.wenchen.yiyi.feature.config.common.ConfigManager
import com.wenchen.yiyi.feature.worldBook.entity.WorldBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ConversationEditActivity : ComponentActivity() {
    private lateinit var conversationId: String
    private var conversationDao = Application.appDatabase.conversationDao()
    private lateinit var imageManager: ImageManager
    private var isCreateNew = mutableStateOf(false)

    private val avatarBitmap = mutableStateOf<Bitmap?>(null)
    private val backgroundBitmap = mutableStateOf<Bitmap?>(null)
    private var hasNewAvatar = mutableStateOf(false)
    private var hasNewBackground = mutableStateOf(false)

    // 头像图片选择器
    private val pickAvatarLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageUri = result.data?.data
                try {
                    val bitmap =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(
                                    contentResolver,
                                    imageUri!!,
                                ),
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                        }
                    // 更新UI状态
                    avatarBitmap.value = bitmap
                    hasNewAvatar.value = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "头像加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // 背景图片选择器
    private val pickBackgroundLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageUri = result.data?.data
                try {
                    val bitmap =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(
                                    contentResolver,
                                    imageUri!!,
                                ),
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                        }
                    // 更新UI状态
                    backgroundBitmap.value = bitmap
                    hasNewBackground.value = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "背景加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageManager = ImageManager()
        conversationId = intent.getStringExtra("CONVERSATION_ID") ?: run {
            isCreateNew.value = true
            UUID.randomUUID().toString()
        }

        setContent {
            AIChatTheme {
                ConversationEditScreen(
                    activity = this,
                    isCreateNew = isCreateNew.value,
                    conversationId = conversationId,
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
                        lifecycleScope.launch {
                            saveConversation(
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
                        }
                    },
                    onCancelClick = { finish() },
                    onAvatarClick = {
                        pickImageFromGallery(true)
                    },
                    onBackgroundClick = {
                        pickImageFromGallery(false)
                    },
                    avatarBitmap = avatarBitmap.value,
                    backgroundBitmap = backgroundBitmap.value,
                    onAvatarDeleteClick = {
                        hasNewAvatar.value = false
                        avatarBitmap.value = null
                    },
                    onBackgroundDeleteClick = {
                        hasNewBackground.value = false
                        backgroundBitmap.value = null
                    },
                )
            }
        }
    }

    private fun pickImageFromGallery(isAvatar: Boolean) {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                    type = "image/*"
                }
            } else {
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            }

        if (isAvatar) {
            pickAvatarLauncher.launch(intent)
        } else {
            pickBackgroundLauncher.launch(intent)
        }
    }

    private suspend fun saveConversation(
        name: String,
        playerName: String,
        playGender: String,
        playerDescription: String,
        chatWorldId: String,
        chatSceneDescription: String,
        additionalSummaryRequirement: String,
        chooseCharacterMap: Map<String, Float>,
        characterKeywordsMap: Map<String, List<String>>,
    ) {
        if (name.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast
                    .makeText(this@ConversationEditActivity, "请输入对话名称", Toast.LENGTH_SHORT)
                    .show()
            }
            return
        }

        withContext(Dispatchers.IO) {
            try {
                // 清理旧图片（如果有新图片被选择）
                if (hasNewAvatar.value) {
                    imageManager.deleteAvatarImage(conversationId)
                    imageManager.saveAvatarImage(conversationId, avatarBitmap.value!!)
                }
                if (hasNewBackground.value) {
                    imageManager.deleteBackgroundImage(conversationId)
                    imageManager.saveBackgroundImage(conversationId, backgroundBitmap.value!!)
                }

                val existingConversation = conversationDao.getById(conversationId)
                if (existingConversation != null) {
                    if (hasNewAvatar.value) {
                        imageManager.deleteAvatarImage(conversationId)
                        imageManager.saveAvatarImage(conversationId, avatarBitmap.value!!)
                    }
                    if (hasNewBackground.value) {
                        imageManager.deleteBackgroundImage(conversationId)
                        imageManager.saveBackgroundImage(conversationId, backgroundBitmap.value!!)
                    }
                    val updatedConversation =
                        existingConversation.copy(
                            name = name,
                            playerName = playerName,
                            playGender = playGender,
                            playerDescription = playerDescription,
                            chatWorldId = chatWorldId,
                            chatSceneDescription = chatSceneDescription,
                            additionalSummaryRequirement = additionalSummaryRequirement,
                            characterIds = chooseCharacterMap,
                            characterKeywords = characterKeywordsMap,
                            avatarPath =
                                if (hasNewAvatar.value) {
                                    imageManager.getAvatarImagePath(
                                        conversationId,
                                    )
                                } else {
                                    existingConversation.avatarPath
                                },
                            backgroundPath =
                                if (hasNewBackground.value) {
                                    imageManager.getBackgroundImagePath(
                                        conversationId,
                                    )
                                } else {
                                    existingConversation.backgroundPath
                                },
                        )

                    val result = conversationDao.update(updatedConversation)

                    withContext(Dispatchers.Main) {
                        if (result > 0) {
                            Toast
                                .makeText(
                                    this@ConversationEditActivity,
                                    "更新成功",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            finish()
                        } else {
                            Toast
                                .makeText(
                                    this@ConversationEditActivity,
                                    "更新失败",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }
                    }
                } else {
                    // 创建新对话的情况
                    val newConversation =
                        Conversation(
                            id = conversationId,
                            name = name,
                            type = ConversationType.GROUP,
                            characterIds = chooseCharacterMap,
                            characterKeywords = characterKeywordsMap,
                            playerName = playerName,
                            playGender = playGender,
                            playerDescription = playerDescription,
                            chatWorldId = chatWorldId,
                            chatSceneDescription = chatSceneDescription,
                            additionalSummaryRequirement = additionalSummaryRequirement,
                            avatarPath =
                                if (hasNewAvatar.value) {
                                    imageManager.getAvatarImagePath(
                                        conversationId,
                                    )
                                } else {
                                    ""
                                },
                            backgroundPath =
                                if (hasNewBackground.value) {
                                    imageManager.getBackgroundImagePath(
                                        conversationId,
                                    )
                                } else {
                                    ""
                                },
                        )

                    conversationDao.insert(newConversation)

                    withContext(Dispatchers.Main) {
                        Toast
                            .makeText(
                                this@ConversationEditActivity,
                                "创建成功",
                                Toast.LENGTH_SHORT,
                            ).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast
                        .makeText(
                            this@ConversationEditActivity,
                            "保存失败: ${e.message}",
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    }
}

// 从文件加载世界书列表
private suspend fun loadWorldBooks(
    adapter: JsonAdapter<WorldBook>,
    onLoaded: (List<WorldBook>) -> Unit
) = withContext(Dispatchers.IO) {
    val worldBookFiles = FilesUtil.listFileNames("world_book")
    Log.d("WorldBookListActivity", "loadWorldBooks: $worldBookFiles")
    val worldBooks = mutableListOf(WorldBook("","未选择世界"))

    worldBookFiles.forEach { fileName ->
        try {
            val json = FilesUtil.readFile("world_book/$fileName")
            val worldBook = adapter.fromJson(json)
            worldBook?.let { worldBooks.add(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    withContext(Dispatchers.Main) {
        onLoaded(worldBooks)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationEditScreen(
    activity: ComponentActivity,
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
    val configManager = ConfigManager()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var playerName by remember { mutableStateOf(configManager.getUserName().toString()) }
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
                coroutineScope.launch {
                    loadWorldBooks(worldBookAdapter) { loadedBooks ->
                        allWorldBook = loadedBooks
                    }
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
                            .clickable { activity.finish() }
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
                    Row(verticalAlignment = Alignment.CenterVertically){
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
                                characterKeywordsMap = characterKeywordsMap + (characterId to emptyList())
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
                    Box(modifier = Modifier.fillMaxWidth()){
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
                                mutableStateOf(characterKeywordsMap[characterId]?.joinToString("/") ?: "")
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
                                            val memoryDao = Application.appDatabase.aiChatMemoryDao()
                                            // 检查是否已存在该角色的记忆
                                            val existingMemory = memoryDao.getByCharacterIdAndConversationId(selectCharacterId, conversationId)

                                            if (existingMemory != null) {
                                                // 更新现有记忆
                                                val updatedMemory = existingMemory.copy(content = newMemoryContent,count = memoryCount)
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
                                                Toast.makeText(context, "角色记忆保存成功", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        text = allWorldBook.find { it.id == chatWorldId }?.worldName ?: "未选择世界",
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
