package com.wenchen.yiyi.feature.aiChat.view

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wenchen.yiyi.feature.aiChat.viewmodel.CharacterEditViewModel
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.wenchen.yiyi.core.designSystem.component.SettingTextFieldItem
import com.wenchen.yiyi.core.common.theme.BlackText
import com.wenchen.yiyi.core.common.theme.DarkGray
import com.wenchen.yiyi.core.common.theme.Gold
import com.wenchen.yiyi.core.common.theme.LightGray
import com.wenchen.yiyi.core.common.theme.WhiteText
import com.wenchen.yiyi.core.util.storage.FilesUtils
import com.wenchen.yiyi.core.util.ui.StatusBarUtils
import com.wenchen.yiyi.core.designSystem.component.SwitchWithText
import com.wenchen.yiyi.feature.worldBook.model.WorldBook
import timber.log.Timber
import androidx.compose.runtime.collectAsState
import com.wenchen.yiyi.core.util.ui.ToastUtils

@Composable
internal fun CharacterEditRoute(
    viewModel: CharacterEditViewModel = hiltViewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importCharacterFromImage(context, it) }
    }
    AIChatTheme {
        CharacterEditScreen(
            viewModel,
            navController,
            onPickImageClick = { launcher.launch("image/*") }
        )
    }
}
@Composable
private fun CharacterEditScreen(
    viewModel: CharacterEditViewModel = hiltViewModel(),
    navController: NavController,
    onPickImageClick: () -> Unit
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

    CharacterEditScreenContent(
        activity = activity,
        isNewCharacter = viewModel.isNewCharacter.collectAsState().value,
        onSaveClick = { name, description, mes_example, memory, memoryCount, playerName, playGender, playerDescription, chatWorldId ->
            viewModel.saveCharacter(
                name = name,
                description = description,
                first_mes = null, // 暂时不添加
                mes_example = mes_example,
                personality = null, // 暂时不添加
                scenario = null, // 暂时不添加
                creator_notes = null, // 暂时不添加
                system_prompt = null, // 暂时不添加
                post_history_instructions = null, // 暂时不添加
                memory = memory,
                memoryCount = memoryCount,
                playerName = playerName,
                playGender = playGender,
                playerDescription = playerDescription,
                chatWorldId = chatWorldId
            )
        },
        onCancelClick = {
            viewModel.navigateBack()
        },
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
        onResetCountClick = {
            ToastUtils.showToast("点击保存以生效")
        },
        onPickImageClick = onPickImageClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterEditScreenContent(
    activity: Activity,
    isNewCharacter: Boolean,
    onSaveClick: (String, String, String?, String, Int, String, String, String, String) -> Unit,
    onCancelClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onBackgroundClick: () -> Unit,
    onResetCountClick: () -> Unit,
    avatarBitmap: Bitmap? = null,
    backgroundBitmap: Bitmap? = null,
    onAvatarDeleteClick: () -> Unit,
    onBackgroundDeleteClick: () -> Unit,
    onPickImageClick: () -> Unit,
    viewModel: CharacterEditViewModel = viewModel()
) {
    if (isSystemInDarkTheme()) {
        StatusBarUtils.setStatusBarTextColor(activity, false)
    } else {
        StatusBarUtils.setStatusBarTextColor(activity, true)
    }
    val parsedCharacter = viewModel.parsedCharacter.collectAsState().value
    val parsedMemory = viewModel.parsedMemory.collectAsState().value

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var mes_example by remember { mutableStateOf("") }
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
    // 监听解析后的角色数据，用于从图片导入角色功能
    LaunchedEffect(parsedCharacter) {
        parsedCharacter?.let { charaData ->
            name = charaData.name
            description = buildString {
                if (charaData.description.isNotEmpty()) {
                    append(charaData.description)
                }
            }.trim()
            mes_example = charaData.mes_example ?: ""
            memory = parsedMemory ?: ""
        }
    }
    // 如果是编辑模式，加载角色数据
    LaunchedEffect(viewModel.characterId.collectAsState().value) {
        if (isNewCharacter) return@LaunchedEffect
        viewModel.loadCharacterData(
            viewModel.conversationId.value,
            viewModel.characterId.value
        ) { character, memoryEntity ->
            name = character?.name ?: ""
            description = character?.description ?: ""
            mes_example = character?.mes_example ?: ""
            memory = memoryEntity?.content ?: ""
            memoryCount = memoryEntity?.count ?: 0
            avatarPath = character?.avatar ?: ""
            backgroundPath = character?.background ?: ""
            Timber.tag("ComposeCharacterEditActivity").d("加载角色数据: ${character?.name}")
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
                        .clickable { viewModel.navigateBack() }
                        .size(18.dp)
                        .align(Alignment.CenterStart)
                )
                Text(
                    text = "角色配置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
                TextButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = { onPickImageClick() }
                ) {
                    Text("导入角色")
                }
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
                            description,
                            mes_example,
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
                    .heightIn(min = 300.dp),
                label = "角色设定",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("请输入角色设定、身份、外貌、行为规则等...") },
                minLines = 15,
                maxLines = 40,
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 250.dp),
                label = "输出示例",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = mes_example,
                onValueChange = { mes_example = it },
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
                    text = "记忆次数: ${memoryCount}",
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

            if (isNewCharacter) {
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
    val worldBookFiles = FilesUtils.listFileNames("world_book")
    Timber.tag("WorldBookListActivity").d("loadWorldBooks: $worldBookFiles")
    val worldBooks = mutableListOf(WorldBook("", "未选择世界"))

    worldBookFiles.forEach { fileName ->
        try {
            val json = FilesUtils.readFile("world_book/$fileName")
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
