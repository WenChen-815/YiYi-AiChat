package com.wenchen.yiyi.feature.aiChat.view

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wenchen.yiyi.core.designSystem.theme.*
import com.wenchen.yiyi.core.ui.SettingTextFieldItem
import com.wenchen.yiyi.core.util.ui.StatusBarUtils
import com.wenchen.yiyi.core.util.ui.ToastUtils
import com.wenchen.yiyi.feature.aiChat.viewmodel.CharacterEditViewModel
import androidx.activity.compose.LocalActivity
import com.wenchen.yiyi.core.designSystem.component.SpaceHorizontalLarge

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
    CharacterEditScreen(
        viewModel = viewModel,
        onPickImageClick = { launcher.launch("image/*") }
    )
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
    val parsedWorldBook by viewModel.parsedWorldBook.collectAsState()
    val saveWorldBookFlag by viewModel.saveWorldBookFlag.collectAsState()

    // 编辑状态
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var mesExample by remember { mutableStateOf("") }
    var memory by remember { mutableStateOf("") }
    var memoryCount by remember { mutableIntStateOf(0) }
    var avatarPath by remember { mutableStateOf("") }
    var backgroundPath by remember { mutableStateOf("") }
    var chatWorldId by remember { mutableStateOf<List<String>>(emptyList()) }

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
        onBackClick = { viewModel.navigateBack() },
        onPickImageClick = onPickImageClick,
        parsedRegexGroupName = parsedRegexGroup?.name,
        savaRegexGroupFlag = savaRegexGroupFlag,
        onSaveRegexGroupChange = viewModel::onSaveRegexGroupChange,
        parsedWorldBookName = parsedWorldBook?.name,
        saveWorldBookFlag = saveWorldBookFlag,
        onSaveWorldBookChange = viewModel::onSaveWorldBookChange
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
    onBackClick: () -> Unit,
    onPickImageClick: () -> Unit,
    parsedRegexGroupName: String?,
    savaRegexGroupFlag: Boolean?,
    onSaveRegexGroupChange: (Boolean) -> Unit,
    parsedWorldBookName: String?,
    saveWorldBookFlag: Boolean?,
    onSaveWorldBookChange: (Boolean) -> Unit
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
                    onClick = onSaveClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("保存角色", color = TextWhite)
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
                label = "角色记忆",
                value = memory,
                onValueChange = onMemoryChange,
                placeholder = { Text("请输入角色记忆内容...") },
                minLines = 3,
                maxLines = 10,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "记忆次数: $memoryCount", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onResetCountClick) {
                    Text("重置次数")
                }
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
                    IconButton(
                        onClick = onAvatarDeleteClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .offset(8.dp, (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RemoveCircleOutline,
                            contentDescription = "删除头像",
                            tint = TextSecondaryLight
                        )
                    }
                } else if (avatarPath.isNotEmpty()) {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "添加头像")
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
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSystemInDarkTheme()) DarkGray else LightGray)
                    .clickable { onBackgroundClick() },
                contentAlignment = Alignment.Center
            ) {
                if (backgroundBitmap != null) {
                    Image(
                        bitmap = backgroundBitmap.asImageBitmap(),
                        contentDescription = "背景图",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = onBackgroundDeleteClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .offset(8.dp, (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RemoveCircleOutline,
                            contentDescription = "删除背景",
                            tint = TextSecondaryLight
                        )
                    }
                } else if (backgroundPath.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(backgroundPath.toUri())
                            .crossfade(true)
                            .build(),
                        contentDescription = "背景图",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "添加背景")
                        Text("添加背景", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (parsedRegexGroupName != null) {
                Text(
                    text = "正则导入",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = savaRegexGroupFlag ?: false,
                        onCheckedChange = { onSaveRegexGroupChange(it) }
                    )
                    Text(
                        text = "保存正则组: $parsedRegexGroupName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (parsedWorldBookName != null) {
                Text(
                    text = "世界书导入",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = saveWorldBookFlag ?: false,
                        onCheckedChange = { onSaveWorldBookChange(it) }
                    )
                    Text(
                        text = "保存世界书: $parsedWorldBookName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(128.dp))
        }
    }
}