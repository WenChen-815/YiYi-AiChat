package com.wenchen.yiyi.feature.aiChat.view

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.copy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.wenchen.yiyi.core.common.theme.*
import com.wenchen.yiyi.core.designSystem.component.SettingTextFieldItem
import com.wenchen.yiyi.core.util.storage.FilesUtils
import com.wenchen.yiyi.core.util.ui.StatusBarUtils
import com.wenchen.yiyi.core.util.ui.ToastUtils
import com.wenchen.yiyi.feature.aiChat.viewmodel.CharacterEditViewModel
import com.wenchen.yiyi.feature.worldBook.model.WorldBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            viewModel = viewModel,
            onPickImageClick = { launcher.launch("image/*") }
        )
    }
}

@Composable
private fun CharacterEditScreen(
    viewModel: CharacterEditViewModel,
    onPickImageClick: () -> Unit
) {
    val activity = LocalActivity.current
    val isNewCharacter by viewModel.isNewCharacter.collectAsState()
    val parsedCharacter by viewModel.parsedCharacter.collectAsState()
    val parsedMemory by viewModel.parsedMemory.collectAsState()
    val characterId by viewModel.characterId.collectAsState()
    val conversationId by viewModel.conversationId.collectAsState()
    val parsedRegexGroup by viewModel.parsedRegexGroup.collectAsState()
    val savaRegexGroupFlag by viewModel.saveRegexGroupFlag.collectAsState()

    // 编辑状态
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var mesExample by remember { mutableStateOf("") }
    var memory by remember { mutableStateOf("") }
    var memoryCount by remember { mutableIntStateOf(0) }
    var avatarPath by remember { mutableStateOf("") }
    var backgroundPath by remember { mutableStateOf("") }
    var chatWorldId by remember { mutableStateOf("") }

    var allWorldBook by remember { mutableStateOf<List<WorldBook>>(emptyList()) }
    var isWorldSheetVisible by remember { mutableStateOf(false) }

    val moshi = remember { Moshi.Builder().build() }
    val worldBookAdapter = remember { moshi.adapter(WorldBook::class.java) }

    // 状态栏
    val isDark = isSystemInDarkTheme()
    SideEffect {
        activity?.let { StatusBarUtils.setStatusBarTextColor(it, !isDark) }
    }

    // 导入解析
    LaunchedEffect(parsedCharacter) {
        parsedCharacter?.let { charaData ->
            name = charaData.name
            description = charaData.description
            mesExample = charaData.mes_example ?: ""
            memory = parsedMemory ?: ""
        }
    }

    // 加载已有数据
    LaunchedEffect(characterId) {
        if (isNewCharacter) return@LaunchedEffect
        viewModel.loadCharacterData(conversationId, characterId) { chara, mem ->
            name = chara?.name ?: ""
            description = chara?.description ?: ""
            mesExample = chara?.mes_example ?: ""
            memory = mem?.content ?: ""
            memoryCount = mem?.count ?: 0
            avatarPath = chara?.avatar ?: ""
            backgroundPath = chara?.background ?: ""
        }
    }

    // 世界书列表
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.viewModelScope.launch {
                    loadWorldBooks(worldBookAdapter) { allWorldBook = it }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val pickAvatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleImageResult(activity!!, result, {
            viewModel.avatarBitmap = it
            viewModel.hasNewAvatar = true
        }, {})
    }

    val pickBackgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleImageResult(activity!!, result, {
            viewModel.backgroundBitmap = it
            viewModel.hasNewBackground = true
        }, {})
    }

    CharacterEditScreenContent(
        isNewCharacter = isNewCharacter,
        name = name,
        onNameChange = { name = it },
        description = description,
        onDescriptionChange = { description = it },
        mesExample = mesExample,
        onMesExampleChange = { mesExample = it },
        memory = memory,
        onMemoryChange = { memory = it },
        memoryCount = memoryCount,
        onResetCountClick = {
            memoryCount = 0
            ToastUtils.showToast("点击保存以生效")
        },
        avatarBitmap = viewModel.avatarBitmap,
        backgroundBitmap = viewModel.backgroundBitmap,
        avatarPath = avatarPath,
        backgroundPath = backgroundPath,
        onAvatarClick = { pickAvatarLauncher.launch(viewModel.createImagePickerIntent()) },
        onAvatarDeleteClick = {
            viewModel.hasNewAvatar = false
            viewModel.avatarBitmap = null
        },
        onBackgroundClick = { pickBackgroundLauncher.launch(viewModel.createImagePickerIntent()) },
        onBackgroundDeleteClick = {
            viewModel.hasNewBackground = false
            viewModel.backgroundBitmap = null
        },
        onSaveClick = {
            viewModel.saveCharacter(
                name, description, null, mesExample, null, null, null, null, null,
                memory, memoryCount, "", "", "", chatWorldId
            )
        },
        onCancelClick = { viewModel.navigateBack() },
        onBackClick = { viewModel.navigateBack() },
        onPickImageClick = onPickImageClick,
        chatWorldId = chatWorldId,
        allWorldBook = allWorldBook,
        isWorldSheetVisible = isWorldSheetVisible,
        onWorldSheetVisibleChange = { isWorldSheetVisible = it },
        onWorldSelected = { chatWorldId = it.id; isWorldSheetVisible = false },
        parsedRegexGroupName = parsedRegexGroup?.name,
        savaRegexGroupFlag = savaRegexGroupFlag,
        onSaveRegexGroupChange = viewModel::onSaveRegexGroupChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterEditScreenContent(
    isNewCharacter: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    mesExample: String,
    onMesExampleChange: (String) -> Unit,
    memory: String,
    onMemoryChange: (String) -> Unit,
    memoryCount: Int,
    onResetCountClick: () -> Unit,
    avatarBitmap: Bitmap?,
    backgroundBitmap: Bitmap?,
    avatarPath: String,
    backgroundPath: String,
    onAvatarClick: () -> Unit,
    onAvatarDeleteClick: () -> Unit,
    onBackgroundClick: () -> Unit,
    onBackgroundDeleteClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onBackClick: () -> Unit,
    onPickImageClick: () -> Unit,
    chatWorldId: String,
    allWorldBook: List<WorldBook>,
    isWorldSheetVisible: Boolean,
    onWorldSheetVisibleChange: (Boolean) -> Unit,
    onWorldSelected: (WorldBook) -> Unit,
    parsedRegexGroupName: String?,
    savaRegexGroupFlag: Boolean?,
    onSaveRegexGroupChange: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()

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
                        .clickable { onBackClick() }
                        .size(18.dp)
                        .align(Alignment.CenterStart)
                )
                Text(
                    text = "角色配置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
                if (isNewCharacter) {
                    TextButton(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onClick = onPickImageClick
                    ) {
                        Text("导入角色")
                    }
                }

            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("取消编辑", color = WhiteText)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "角色名称",
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("请输入角色名称") }
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(min = 300.dp),
                label = "角色设定",
                value = description,
                onValueChange = onDescriptionChange,
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
                value = mesExample,
                onValueChange = onMesExampleChange,
                placeholder = { Text("请输入输出示例...") },
                minLines = 3,
                maxLines = 10,
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 250.dp),
                label = "长期记忆",
                value = memory,
                onValueChange = onMemoryChange,
                placeholder = { Text("暂无长期记忆") },
                minLines = 5,
                maxLines = 15,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "记忆次数: $memoryCount", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "重置次数",
                    style = MaterialTheme.typography.bodyMedium.copy(Gold),
                    modifier = Modifier.clickable { onResetCountClick() }
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
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onAvatarDeleteClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .offset(8.dp, (-8).dp)
                    ) {
                        Icon(
                            Icons.Rounded.RemoveCircleOutline,
                            contentDescription = null,
                            tint = BlackText
                        )
                    }
                } else if (avatarPath.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(avatarPath.toUri())
                            .crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("添加头像", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

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
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onBackgroundDeleteClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .offset(8.dp, (-8).dp)
                    ) {
                        Icon(
                            Icons.Rounded.RemoveCircleOutline,
                            contentDescription = null,
                            tint = BlackText
                        )
                    }
                } else if (backgroundPath.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(backgroundPath.toUri()).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("添加背景", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (savaRegexGroupFlag != null) {
                Text(
                    text = "正则组",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = parsedRegexGroupName ?: "未知正则组",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                            color = WhiteText
                        )
                        // 使用自定义的圆形勾选框
                        RoundCheckbox(
                            checked = savaRegexGroupFlag,
                            onCheckedChange = { onSaveRegexGroupChange(it) },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }
            }
            if (isNewCharacter) {
                Text(
                    text = "世界选择",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onWorldSheetVisibleChange(true) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = allWorldBook.find { it.id == chatWorldId }?.worldName
                            ?: "未选择世界",
                        modifier = Modifier.fillMaxWidth(),
                        color = WhiteText
                    )
                }
            }

            if (isWorldSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = { onWorldSheetVisibleChange(false) },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
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
                                modifier = Modifier.clickable { onWorldSelected(worldBook) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .border(
                width = 1.5.dp,
                color = Color.White,
                shape = CircleShape
            ) // 白色边框
            .clip(CircleShape)
            .background(if (checked) Color.White.copy(alpha = 0.3f) else Color.Transparent) // 勾选后的填充感（半透明白以突出白勾）
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check, // 白色的勾
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterEditPreview() {
    AIChatTheme {
        CharacterEditScreenContent(
            isNewCharacter = true,
            name = "预览角色",
            onNameChange = {},
            description = "这是一个测试角色的描述设定。",
            onDescriptionChange = {},
            mesExample = "预览对话示例",
            onMesExampleChange = {},
            memory = "长期记忆内容",
            onMemoryChange = {},
            memoryCount = 5,
            onResetCountClick = {},
            avatarBitmap = null,
            backgroundBitmap = null,
            avatarPath = "",
            backgroundPath = "",
            onAvatarClick = {},
            onAvatarDeleteClick = {},
            onBackgroundClick = {},
            onBackgroundDeleteClick = {},
            onSaveClick = {},
            onCancelClick = {},
            onBackClick = {},
            onPickImageClick = {},
            chatWorldId = "",
            allWorldBook = listOf(WorldBook("1", "现代都市"), WorldBook("2", "魔法世界")),
            isWorldSheetVisible = false,
            onWorldSheetVisibleChange = {},
            onWorldSelected = {},
            parsedRegexGroupName = null,
            savaRegexGroupFlag = null,
            onSaveRegexGroupChange = {}
        )
    }
}

private suspend fun loadWorldBooks(
    adapter: JsonAdapter<WorldBook>,
    onLoaded: (List<WorldBook>) -> Unit
) = withContext(Dispatchers.IO) {
    val worldBookFiles = FilesUtils.listFileNames("world_book")
    val worldBooks = mutableListOf(WorldBook("", "未选择世界"))
    worldBookFiles.forEach { fileName ->
        try {
            val json = FilesUtils.readFile("world_book/$fileName")
            adapter.fromJson(json)?.let { worldBooks.add(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    withContext(Dispatchers.Main) { onLoaded(worldBooks) }
}
