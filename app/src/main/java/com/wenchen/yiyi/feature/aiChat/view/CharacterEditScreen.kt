package com.wenchen.yiyi.feature.aiChat.view

import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wenchen.yiyi.core.designSystem.theme.*
import com.wenchen.yiyi.core.ui.BottomGradientButton
import com.wenchen.yiyi.core.ui.SettingTextFieldItem
import com.wenchen.yiyi.core.util.ui.StatusBarUtils
import com.wenchen.yiyi.core.util.ui.ToastUtils
import com.wenchen.yiyi.feature.aiChat.viewmodel.CharacterEditViewModel

@Composable
internal fun CharacterEditRoute(
    viewModel: CharacterEditViewModel = hiltViewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    // 角色卡文件导入启动器
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

    // 观察 ViewModel 中的状态流
    val isNewCharacter by viewModel.isNewCharacter.collectAsState()
    val character by viewModel.currentCharacter.collectAsState()
    val memory by viewModel.memory.collectAsState()
    val memoryCount by viewModel.memoryCount.collectAsState()

    // 导入扩展相关的状态
    val parsedRegexGroup by viewModel.parsedRegexGroup.collectAsState()
    val savaRegexGroupFlag by viewModel.saveRegexGroupFlag.collectAsState()
    val parsedWorldBook by viewModel.parsedWorldBook.collectAsState()
    val saveWorldBookFlag by viewModel.saveWorldBookFlag.collectAsState()

    // 动态调整状态栏文字颜色（深浅色模式切换）
    val isDark = isSystemInDarkTheme()
    SideEffect {
        activity?.let { StatusBarUtils.setStatusBarTextColor(it, !isDark) }
    }

    // 头像选择启动器
    val pickAvatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleImageResult(activity!!, result, {
            viewModel.avatarBitmap = it
            viewModel.hasNewAvatar = true
        }, { error -> ToastUtils.showToast(error) })
    }

    // 背景图选择启动器
    val pickBackgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleImageResult(activity!!, result, {
            viewModel.backgroundBitmap = it
            viewModel.hasNewBackground = true
        }, { error -> ToastUtils.showToast(error) })
    }

    CharacterEditScreenContent(
        isNewCharacter = isNewCharacter,
        // UI 数据源直接来自 ViewModel 状态
        name = character?.name ?: "",
        onNameChange = { newName ->
            viewModel.updateCharacter { it.copy(name = newName) }
        },
        description = character?.description ?: "",
        onDescriptionChange = { newDesc ->
            viewModel.updateCharacter { it.copy(description = newDesc) }
        },
        mesExample = character?.mes_example ?: "",
        onMesExampleChange = { newEx ->
            viewModel.updateCharacter { it.copy(mes_example = newEx) }
        },
        memory = memory,
        onMemoryChange = { viewModel.updateMemory(it) },
        memoryCount = memoryCount,
        onResetCountClick = {
            viewModel.updateMemoryCount(0)
            ToastUtils.showToast("次数已清零，保存后生效")
        },
        avatarBitmap = viewModel.avatarBitmap,
        backgroundBitmap = viewModel.backgroundBitmap,
        avatarPath = character?.avatar ?: "",
        backgroundPath = character?.background ?: "",
        onAvatarClick = { pickAvatarLauncher.launch(viewModel.createImagePickerIntent()) },
        onAvatarDeleteClick = {
            viewModel.hasNewAvatar = true // 标记有变动
            viewModel.avatarBitmap = null
        },
        onBackgroundClick = { pickBackgroundLauncher.launch(viewModel.createImagePickerIntent()) },
        onBackgroundDeleteClick = {
            viewModel.hasNewBackground = true // 标记有变动
            viewModel.backgroundBitmap = null
        },
        onSaveClick = { viewModel.saveCharacter() },
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
            TopAppBar(
                title = { Text("角色配置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "返回", modifier = Modifier.size(18.dp))
                    }
                },
                actions = {
                    if (isNewCharacter) {
                        TextButton(onClick = onPickImageClick) {
                            Text("导入角色")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomGradientButton(
                onClick = onSaveClick,
                text = "保存角色"
            )
        },
        modifier = Modifier.fillMaxSize().imePadding() // 自动处理软键盘遮挡
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // --- 基础信息编辑 ---
            SettingTextFieldItem(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = "角色名称",
                value = name,
                onValueChange = onNameChange,
                singleLine = true
            )

            SettingTextFieldItem(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 200.dp),
                label = "角色设定",
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = { Text("输入角色设定、背景和行为准则...") },
                minLines = 8
            )

            SettingTextFieldItem(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                label = "对话示例",
                value = mesExample,
                onValueChange = onMesExampleChange,
                minLines = 4
            )

            SettingTextFieldItem(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                label = "角色记忆",
                value = memory,
                onValueChange = onMemoryChange,
                placeholder = { Text("此处内容会长期保留在上下文末尾...") },
                minLines = 3
            )

            // --- 记忆管理 ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("当前记忆轮数", style = MaterialTheme.typography.labelMedium)
                        Text("$memoryCount 轮", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedButton(onClick = onResetCountClick) {
                        Text("重置次数")
                    }
                }
            }

            // --- 媒体内容 ---
            SectionTitle("角色头像")
            ImagePickerBox(
                bitmap = avatarBitmap,
                path = avatarPath,
                modifier = Modifier.size(120.dp),
                onClick = onAvatarClick,
                onDelete = onAvatarDeleteClick
            )

            SectionTitle("对话背景")
            ImagePickerBox(
                bitmap = backgroundBitmap,
                path = backgroundPath,
                modifier = Modifier.fillMaxWidth().height(160.dp),
                onClick = onBackgroundClick,
                onDelete = onBackgroundDeleteClick,
                contentScale = ContentScale.FillWidth
            )

            // --- 导入扩展选项 ---
            if (parsedRegexGroupName != null) {
                ExtensionSaveOption(
                    title = "保存正则组: $parsedRegexGroupName",
                    checked = savaRegexGroupFlag ?: false,
                    onCheckedChange = onSaveRegexGroupChange
                )
            }

            if (parsedWorldBookName != null) {
                ExtensionSaveOption(
                    title = "保存世界书: $parsedWorldBookName",
                    checked = saveWorldBookFlag ?: false,
                    onCheckedChange = onSaveWorldBookChange
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * 通用的图片选择/展示组件
 */
@Composable
private fun ImagePickerBox(
    bitmap: Bitmap?,
    path: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSystemInDarkTheme()) DarkGray else LightGray)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            // 优先显示新选择的位图
            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
                DeleteIconButton(onDelete)
            }
            // 其次显示已有路径的图片
            path.isNotEmpty() -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(path.toUri())
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
                // 已有图片也可以通过点击重新选择来覆盖，不提供直接删除以防逻辑混乱，建议覆盖
            }
            // 最后显示占位符
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("点击上传", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.DeleteIconButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .size(24.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
    ) {
        Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "移除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun ExtensionSaveOption(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, name = "新建角色预览")
@Composable
private fun CharacterEditNewPreview() {
    AppTheme {
        CharacterEditScreenContent(
            isNewCharacter = true,
            name = "",
            onNameChange = {},
            description = "",
            onDescriptionChange = {},
            mesExample = "",
            onMesExampleChange = {},
            memory = "",
            onMemoryChange = {},
            memoryCount = 0,
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
            onBackClick = {},
            onPickImageClick = {},
            parsedRegexGroupName = null,
            savaRegexGroupFlag = false,
            onSaveRegexGroupChange = {},
            parsedWorldBookName = null,
            saveWorldBookFlag = false,
            onSaveWorldBookChange = {}
        )
    }
}

@Preview(showBackground = true, name = "编辑已有角色预览")
@Composable
private fun CharacterEditExistPreview() {
    AppTheme() {
        CharacterEditScreenContent(
            isNewCharacter = false,
            name = "爱莉丝",
            onNameChange = {},
            description = "性格温柔，是一名来自异世界的魔法师...",
            onDescriptionChange = {},
            mesExample = "爱莉丝：你好，旅行者。今天有什么我可以帮你的吗？",
            onMesExampleChange = {},
            memory = "我们昨天在森林里遇到了一只受伤的小猫。",
            onMemoryChange = {},
            memoryCount = 12,
            onResetCountClick = {},
            avatarBitmap = null,
            backgroundBitmap = null,
            avatarPath = "dummy_path",
            backgroundPath = "dummy_bg_path",
            onAvatarClick = {},
            onAvatarDeleteClick = {},
            onBackgroundClick = {},
            onBackgroundDeleteClick = {},
            onSaveClick = {},
            onBackClick = {},
            onPickImageClick = {},
            parsedRegexGroupName = "角色专用正则",
            savaRegexGroupFlag = true,
            onSaveRegexGroupChange = {},
            parsedWorldBookName = "魔法世界设定集",
            saveWorldBookFlag = true,
            onSaveWorldBookChange = {}
        )
    }
}