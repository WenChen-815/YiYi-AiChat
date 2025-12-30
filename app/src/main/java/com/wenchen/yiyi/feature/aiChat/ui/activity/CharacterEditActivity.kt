package com.wenchen.yiyi.feature.aiChat.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wenchen.yiyi.feature.aiChat.vm.CharacterEditViewModel
import com.wenchen.yiyi.App
import com.wenchen.yiyi.core.common.entity.AICharacter
import com.wenchen.yiyi.feature.aiChat.entity.AIChatMemory
import com.wenchen.yiyi.feature.config.common.ConfigManager
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.wenchen.yiyi.feature.aiChat.common.ImageManager
import com.wenchen.yiyi.feature.aiChat.entity.Conversation
import com.wenchen.yiyi.feature.aiChat.entity.ConversationType
import com.wenchen.yiyi.core.common.components.SettingTextFieldItem
import com.wenchen.yiyi.core.common.theme.BlackText
import com.wenchen.yiyi.core.common.theme.DarkGray
import com.wenchen.yiyi.core.common.theme.Gold
import com.wenchen.yiyi.core.common.theme.LightGray
import com.wenchen.yiyi.core.common.theme.WhiteText
import com.wenchen.yiyi.core.common.utils.FilesUtil
import com.wenchen.yiyi.core.common.utils.StatusBarUtil
import com.wenchen.yiyi.feature.config.ui.SwitchWithText
import com.wenchen.yiyi.feature.worldBook.entity.WorldBook

class CharacterEditActivity : ComponentActivity(), CoroutineScope by MainScope() {
    private var characterId: String? = null
    private var conversationId: String? = null
    private var aiCharacterDao = App.appDatabase.aiCharacterDao()
    private var aiChatMemoryDao = App.appDatabase.aiChatMemoryDao()
    private var conversationDao = App.appDatabase.conversationDao()
    private val userId = ConfigManager().getUserId().toString()
    private lateinit var imageManager: ImageManager
    private var isNewCharacter = mutableStateOf(false)

    // 头像图片选择器
    private val pickAvatarLauncher = registerForActivityResult(
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
    private val pickBackgroundLauncher = registerForActivityResult(
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

    // 用于存储选择的图片
    private val avatarBitmap = mutableStateOf<Bitmap?>(null)
    private val backgroundBitmap = mutableStateOf<Bitmap?>(null)

    // 用于跟踪是否有新图片选择的变量
    private var hasNewAvatar = mutableStateOf(false)
    private var hasNewBackground = mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageManager = ImageManager()
        // 检查是否是编辑模式
        characterId = intent.getStringExtra("CHARACTER_ID")
        if (characterId == null) {
            characterId = UUID.randomUUID().toString()
            isNewCharacter.value = true
        }
        conversationId = intent.getStringExtra("CONVERSATION_ID")
            ?: "${ConfigManager().getUserId()}_$characterId"
        Log.d("ComposeCharacterEditActivity", "characterId: $characterId")

        setContent {
            AIChatTheme {
                CharacterEditScreen(
                    activity = this,
                    conversationId = conversationId.toString(),
                    characterId = characterId,
                    isNewCharacter = isNewCharacter.value,
                    onSaveClick = { name, roleIdentity, roleAppearance, roleDescription, outputExample, behaviorRules, memory, memoryCount, playerName, playGender, playerDescription, chatWorldId ->
                        lifecycleScope.launch {
                            saveCharacter(
                                name,
                                roleIdentity,
                                roleAppearance,
                                roleDescription,
                                outputExample,
                                behaviorRules,
                                memory,
                                memoryCount,
                                playerName,
                                playGender,
                                playerDescription,
                                chatWorldId
                            )
                        }
                    },
                    onCancelClick = {
                        finish()
                    },
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
                    onResetCountClick = {
                        Toast.makeText(this, "点击保存以生效", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }

    // 保存角色数据
    private suspend fun saveCharacter(
        name: String,
        roleIdentity: String,
        roleAppearance: String,
        roleDescription: String,
        outputExample: String,
        behaviorRules: String,
        memory: String,
        memoryCount: Int,
        playerName: String,
        playGender: String,
        playerDescription: String,
        chatWorldId: String
    ) {
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入角色名称", Toast.LENGTH_SHORT).show()
            return
        }

        // 清理旧图片（如果有新图片被选择）
        if (hasNewAvatar.value) {
            imageManager.deleteAvatarImage(characterId!!)
            imageManager.saveAvatarImage(characterId!!, avatarBitmap.value!!)
        }
        if (hasNewBackground.value) {
            imageManager.deleteBackgroundImage(characterId!!)
            imageManager.saveBackgroundImage(characterId!!, backgroundBitmap.value!!)
        }

        val character = AICharacter(
            aiCharacterId = characterId!!,
            name = name,
            roleIdentity = roleIdentity,
            roleAppearance = roleAppearance,
            roleDescription = roleDescription,
            outputExample = outputExample,
            behaviorRules = behaviorRules,
            userId = userId,
            createdAt = System.currentTimeMillis(),
            avatarPath = imageManager.getAvatarImagePath(characterId!!),
            backgroundPath = imageManager.getBackgroundImagePath(characterId!!)
        )

        var memoryEntity = aiChatMemoryDao.getByCharacterIdAndConversationId(
            character.aiCharacterId,
            conversationId.toString()
        )
        if (memoryEntity == null) {
            memoryEntity = AIChatMemory(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId.toString(),
                characterId = character.aiCharacterId,
                content = memory.ifEmpty { "" },
                createdAt = System.currentTimeMillis(),
                count = memoryCount
            )
        } else {
            memoryEntity.content = memory.ifEmpty { "" }
            memoryEntity.count = memoryCount
        }

        launch(Dispatchers.IO) {
            try {
                if (isNewCharacter.value) {
                    // 新增角色
                    val result = aiCharacterDao.insertAICharacter(character)
                    val result2 = aiChatMemoryDao.insert(memoryEntity)

                    // 创建初始Conversation
                    val initialConversation = Conversation(
                        id = conversationId.toString(),
                        name = character.name,
                        type = ConversationType.SINGLE,
                        characterIds = mapOf(character.aiCharacterId to 1.0f),
                        characterKeywords = mapOf(character.aiCharacterId to emptyList()),
                        playerName = playerName.ifEmpty { ConfigManager().getUserName().toString() },
                        playGender = playGender,
                        playerDescription = playerDescription,
                        chatWorldId = chatWorldId,
                        chatSceneDescription = "",
                        additionalSummaryRequirement = "",
                        avatarPath = character.avatarPath,
                        backgroundPath = character.backgroundPath,
                    )
                    conversationDao.insert(initialConversation) // 保存初始对话

                    withContext(Dispatchers.Main) {
                        if (result > 0 && result2 > 0) {
                            Toast.makeText(
                                this@CharacterEditActivity,
                                "添加成功",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@CharacterEditActivity,
                                "添加失败",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        finish()
                    }
                } else {
                    // 更新角色
                    val result = aiCharacterDao.updateAICharacter(character)
                    val result2 = aiChatMemoryDao.update(memoryEntity)
                    withContext(Dispatchers.Main) {
                        if (result > 0 && result2 > 0) {
                            Toast.makeText(
                                this@CharacterEditActivity,
                                "更新成功",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@CharacterEditActivity,
                                "更新失败",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CharacterEditActivity,
                        "保存失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun pickImageFromGallery(isAvatar: Boolean) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditScreen(
    activity: ComponentActivity,
    conversationId: String,
    characterId: String? = null,
    isNewCharacter: Boolean,
    onSaveClick: (String, String, String, String, String, String, String, Int, String, String, String, String) -> Unit,
    onCancelClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onBackgroundClick: () -> Unit,
    onResetCountClick: () -> Unit,
    avatarBitmap: Bitmap? = null,
    backgroundBitmap: Bitmap? = null,
    onAvatarDeleteClick: () -> Unit,
    onBackgroundDeleteClick: () -> Unit,
    viewModel: CharacterEditViewModel = viewModel()
) {
    if (isSystemInDarkTheme()) {
        StatusBarUtil.setStatusBarTextColor(activity, false)
    } else {
        StatusBarUtil.setStatusBarTextColor(activity, true)
    }
    var name by remember { mutableStateOf("") }
    var roleIdentity by remember { mutableStateOf("") }
    var roleAppearance by remember { mutableStateOf("") }
    var roleDescription by remember { mutableStateOf("") }
    var outputExample by remember { mutableStateOf("") }
    var behaviorRules by remember { mutableStateOf("") }
    var memory by remember { mutableStateOf("") }
    var memoryCount by remember { mutableIntStateOf(0) }
    var avatarPath by remember { mutableStateOf("") }
    var backgroundPath by remember { mutableStateOf("") }

    // 对话设定
    var playerName by remember { mutableStateOf("") }
    var playGender by remember { mutableStateOf("") }
    var playerDescription by remember { mutableStateOf("") }
    var chatWorldId by remember { mutableStateOf("") }
    var allWorldBook by remember { mutableStateOf<List<WorldBook>>(emptyList()) }
    val (isWorldSheetVisible, setWorldSheetVisible) = remember { mutableStateOf(false) }
    val (isPresetPlayerAndWorld, setPresetPlayerAndWorld) = remember { mutableStateOf(false) }
    val moshi: Moshi = Moshi.Builder().build()
    val worldBookAdapter: JsonAdapter<WorldBook> = moshi.adapter(WorldBook::class.java)


    val scrollState = rememberScrollState()

    // 如果是编辑模式，加载角色数据
    LaunchedEffect(characterId) {
        if (characterId != null) {
            viewModel.loadCharacterData(conversationId, characterId) { character, memoryEntity ->
                name = character?.name ?: ""
                roleIdentity = character?.roleIdentity ?: ""
                roleAppearance = character?.roleAppearance ?: ""
                roleDescription = character?.roleDescription ?: ""
                outputExample = character?.outputExample ?: ""
                behaviorRules = character?.behaviorRules
                    ?: "- 在 。 ？ ！ … 等表示句子结束处，或根于语境需要使用反斜线 (\\) 分隔,以确保良好的可读性与表达\n- 使用[]描述心理、场景或动作，但严格要求[]中的内容不允许使用分隔符(\\)"
                memory = memoryEntity?.content ?: ""
                memoryCount = memoryEntity?.count ?: 0
                avatarPath = character?.avatarPath ?: ""
                backgroundPath = character?.backgroundPath ?: ""
                Log.d("ComposeCharacterEditActivity", "加载角色数据: ${character?.name}")
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "返回",
                    modifier = Modifier
                        .clickable { activity.finish() }
                        .size(18.dp)
                        .align(Alignment.CenterStart)
                )
                Text(
                    text = "角色配置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        bottomBar = {
            // 操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("取消编辑", color = WhiteText)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        onSaveClick(
                            name,
                            roleIdentity,
                            roleAppearance,
                            roleDescription,
                            outputExample,
                            behaviorRules,
                            memory,
                            memoryCount,
                            playerName,
                            playGender,
                            playerDescription,
                            chatWorldId
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("保存角色", color = WhiteText)
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "角色名称",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("请输入角色名称") }
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 250.dp),
                label = "角色任务&身份",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = roleIdentity,
                onValueChange = { roleIdentity = it },
                placeholder = { Text("例: 你需要扮演指定角色，根据角色信息和记忆内容，模仿所扮演的语气进行场景中的对话。你将扮演一位铁面无私的指月集团女总裁顾依依，是指月集团董事长的独女。") },
                minLines = 3,
                maxLines = 10,
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 250.dp),
                label = "角色外貌",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = roleAppearance,
                onValueChange = { roleAppearance = it },
                placeholder = { Text("例: 身高171cm，身形挺拔舒展。质感上乘的浅沙色单扣西装外套，内搭无袖浅卡其色连衣短裙，搭配卡其色高跟鞋。姿态优雅从容，整体散发女总裁御姐的自信与魅力。") },
                minLines = 3,
                maxLines = 10,
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 250.dp),
                label = "角色描述",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = roleDescription,
                onValueChange = { roleDescription = it },
                placeholder = { Text("例: (别看了,这里开始自己发挥吧)") },
                minLines = 5,
                maxLines = 15,
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 250.dp),
                label = "输出示例",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = outputExample,
                onValueChange = { outputExample = it },
                placeholder = {
                    Text(
                        """
                    （带分隔符）
                    1.[听到你赞我新得的墨兰开得雅致，眼中泛起笑意，指尖轻轻拂过花瓣] \ 这花是晨间才开的 \ 你瞧，这瓣上的露水还未干呢
                    （不带分隔符）
                    1.[听到你赞我新得的墨兰开得雅致，眼中泛起笑意，指尖轻轻拂过花瓣] 这花是晨间才开的。你瞧，这瓣上的露水还未干呢""".trimIndent()
                    )
                },
                minLines = 3,
                maxLines = 10,
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 250.dp),
                label = {
                    Column {
                        Text(
                            text = "行为规则",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (isNewCharacter) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "（预设模板，配合全局配置中的分隔符开关使用哦~）",
                                style = MaterialTheme.typography.titleSmall.copy(Color.LightGray)
                            )
                        }

                    }
                },
                labelPadding = PaddingValues(bottom = 6.dp),
                value = behaviorRules,
                onValueChange = { behaviorRules = it },
                placeholder = { Text("请输入角色行为规则，建议分行分点") },
                minLines = 5,
                maxLines = 15,
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 250.dp),
                label = "长期记忆",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = memory,
                onValueChange = { memory = it },
                placeholder = { Text("暂无长期记忆") },
                minLines = 5,
                maxLines = 15,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "记忆次数: $memoryCount",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "重置次数",
                    style = MaterialTheme.typography.bodyMedium.copy(Gold),
                    modifier = Modifier
                        .padding(5.dp)
                        .clickable {
                            memoryCount = 0
                            onResetCountClick()
                        }
                )
            }

            Text(
                text = "角色头像",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSystemInDarkTheme()) DarkGray else LightGray)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "角色头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // 删除头像按钮
                    IconButton(
                        onClick = { onAvatarDeleteClick() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .offset(8.dp, (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RemoveCircleOutline,
                            contentDescription = "删除头像",
                            tint = BlackText
                        )
                    }
                } else if (avatarPath.isNotEmpty()) {
                    // 使用 Coil 加载已有头像
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarPath.toUri())
                            .crossfade(true)
                            .build(),
                        contentDescription = "角色头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 显示默认头像或添加图标
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加头像"
                        )
                        Text("添加头像", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 聊天背景
            Text(
                text = "聊天背景",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSystemInDarkTheme()) DarkGray else LightGray)
                    .clickable { onBackgroundClick() },
                contentAlignment = Alignment.Center
            ) {
                if (backgroundBitmap != null) {
                    Image(
                        bitmap = backgroundBitmap.asImageBitmap(),
                        contentDescription = "聊天背景",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // 删除背景按钮
                    IconButton(
                        onClick = { onBackgroundDeleteClick() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .offset(8.dp, (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RemoveCircleOutline,
                            contentDescription = "删除背景",
                            tint = BlackText
                        )
                    }
                } else if (backgroundPath.isNotEmpty()) {
                    // 使用 Coil 加载已有背景
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(backgroundPath.toUri())
                            .crossfade(true)
                            .build(),
                        contentDescription = "聊天背景",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 显示默认背景或添加图标
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加背景"
                        )
                        Text("添加背景", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if(isNewCharacter){
                SwitchWithText(
                    modifier = Modifier.fillMaxWidth(),
                    text = "预设“我”和世界",
                    checked = isPresetPlayerAndWorld,
                    onCheckedChange = { setPresetPlayerAndWorld(it) }
                )
            }

            if (isPresetPlayerAndWorld) {
                SettingTextFieldItem(
                    modifier = Modifier
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    label = "我的性别",
                    labelPadding = PaddingValues(bottom = 6.dp),
                    value = playGender,
                    onValueChange = { playGender = it },
                    placeholder = { Text("请输入性别") },
                )

                SettingTextFieldItem(
                    modifier = Modifier
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