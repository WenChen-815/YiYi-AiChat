package com.wenchen.yiyi.config.ui.activity

import android.app.Activity.RESULT_OK
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
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wenchen.yiyi.aiChat.common.AIChatManager
import com.wenchen.yiyi.config.common.ConfigManager
import com.wenchen.yiyi.common.ApiService
import com.wenchen.yiyi.aiChat.common.ImageManager
import com.wenchen.yiyi.common.entity.Model
import com.wenchen.yiyi.common.theme.AIChatTheme
import com.wenchen.yiyi.common.theme.BlackBg
import com.wenchen.yiyi.common.theme.Gold
import com.wenchen.yiyi.common.theme.Pink
import com.wenchen.yiyi.common.theme.WhiteBg
import com.wenchen.yiyi.common.theme.WhiteText
import com.wenchen.yiyi.common.utils.StatusBarUtil
import com.wenchen.yiyi.common.components.SettingTextFieldItem
import com.wenchen.yiyi.common.theme.BlackText
import com.wenchen.yiyi.common.theme.DarkGray
import com.wenchen.yiyi.common.theme.GrayText
import com.wenchen.yiyi.common.theme.LightGray
import com.wenchen.yiyi.config.ui.SwitchWithText
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import java.io.File

class ConfigActivity : ComponentActivity() {
    private lateinit var configManager: ConfigManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configManager = ConfigManager()

        setContent {
            AIChatTheme {
                ConfigScreen(configManager, this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(configManager: ConfigManager, activity: ComponentActivity) {
    if (isSystemInDarkTheme()) {
        StatusBarUtil.setStatusBarTextColor(activity, false)
    } else {
        StatusBarUtil.setStatusBarTextColor(activity, true)
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var userId by remember { mutableStateOf(configManager.getUserId() ?: "123123") }
    var userName by remember { mutableStateOf(configManager.getUserName() ?: "温辰") }

    // 对话模型配置
    var apiKey by remember { mutableStateOf(configManager.getApiKey() ?: "") }
    var baseUrl by remember { mutableStateOf(configManager.getBaseUrl() ?: "") }
    var models by remember { mutableStateOf<List<Model>>(emptyList()) }
    var selectedModel by remember { mutableStateOf(configManager.getSelectedModel() ?: "") }
    var isLoadingModels by remember { mutableStateOf(false) }

    // 图片识别配置
    var imgRecognitionEnabled by remember { mutableStateOf(configManager.isImgRecognitionEnabled()) }
    var imgApiKey by remember { mutableStateOf(configManager.getImgApiKey() ?: "") }
    var imgBaseUrl by remember { mutableStateOf(configManager.getImgBaseUrl() ?: "") }
    var imgModels by remember { mutableStateOf<List<Model>>(emptyList()) }
    var selectedImgModel by remember { mutableStateOf(configManager.getSelectedImgModel() ?: "") }
    var isLoadingImgModels by remember { mutableStateOf(false) }

    // 其他配置
    var maxContextCount by remember { mutableStateOf(configManager.getMaxContextMessageSize().toString()) }
    var summarizeCount by remember { mutableStateOf(configManager.getSummarizeTriggerCount().toString()) }
    var maxSummarizeCount by remember { mutableStateOf(configManager.getMaxSummarizeCount().toString()) }
    var enableSeparator by remember { mutableStateOf(configManager.isSeparatorEnabled()) }
    var enableTimePrefix by remember { mutableStateOf(configManager.isTimePrefixEnabled()) }

    // 用户头像相关
    var userAvatarPath by remember { mutableStateOf(configManager.getUserAvatarPath()) }
    val userAvatarBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val hasNewUserAvatar = remember { mutableStateOf(false) }

    val (isChatModelSheetVisible, setChatModelSheetVisible) = remember { mutableStateOf(false) }
    val (isImgModelSheetVisible, setImgModelSheetVisible) = remember { mutableStateOf(false) }
    val sheetModels = remember { mutableStateOf<List<Model>>(emptyList()) }
    val onModelSelectedCallback = remember { mutableStateOf<((String) -> Unit)?>(null) }

    val pickUserAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(activity.contentResolver, imageUri!!))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(activity.contentResolver, imageUri)
                }
                userAvatarBitmap.value = bitmap
                hasNewUserAvatar.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(activity, "头像加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 选择图片函数
    fun pickUserAvatarFromGallery() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
            }
        } else {
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        pickUserAvatarLauncher.launch(intent)
    }

    // 保存用户头像函数
    fun saveUserAvatar() {
        if (hasNewUserAvatar.value && userAvatarBitmap.value != null) {
            // 删除旧头像文件
            val oldAvatarPath = configManager.getUserAvatarPath()
            if (!oldAvatarPath.isNullOrEmpty()) {
                try {
                    val oldFile = File(oldAvatarPath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 保存新头像
            val imageManager = ImageManager(activity)
            val userId = configManager.getUserId() ?: "default_user"
            val savedFile = imageManager.saveAvatarImage("user_$userId", userAvatarBitmap.value!!)

            if (savedFile != null) {
                configManager.saveUserAvatarPath(savedFile.toUri().toString())
                userAvatarPath = savedFile.toUri().toString()
            }
        }
    }

    LaunchedEffect(Unit) {
        // 自动加载对话模型
        if (apiKey.isNotEmpty() && baseUrl.isNotEmpty()) {
            loadModels(
                apiKey = apiKey,
                baseUrl = baseUrl,
                type = 0,
                coroutineScope = coroutineScope,
                setLoading = { isLoadingModels = it },
                setModels = { models = it },
                setSelectedModel = { selectedModel = it },
                configManager = configManager
            )
        }

        // 如果图片识别功能已启用，则自动加载图片识别模型
        if (imgRecognitionEnabled && imgApiKey.isNotEmpty() && imgBaseUrl.isNotEmpty()) {
            loadModels(
                apiKey = imgApiKey,
                baseUrl = imgBaseUrl,
                type = 1,
                coroutineScope = coroutineScope,
                setLoading = { isLoadingImgModels = it },
                setModels = { imgModels = it },
                setSelectedModel = { selectedImgModel = it },
                configManager = configManager
            )
        }
    }
    BackHandler(enabled = isChatModelSheetVisible || isImgModelSheetVisible) {
        setChatModelSheetVisible(false)
        setImgModelSheetVisible(false)
    }
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 12.dp),
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
                    text = "API 配置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        // 保存配置
                        if (apiKey.isEmpty() || baseUrl.isEmpty()) {
                            Toast.makeText(
                                context,
                                "请填写完整的API Key和中转地址",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            return@TextButton
                        }

                        if (models.isEmpty() || selectedModel.isEmpty()) {
                            Toast.makeText(context, "请先加载并选择模型", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        if (imgRecognitionEnabled) {
                            if (imgApiKey.isEmpty() || imgBaseUrl.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "请填写完整的图片识别API Key和中转地址",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@TextButton
                            }

                            if (imgModels.isEmpty() || selectedImgModel.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "请先加载并选择图片识别模型",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                return@TextButton
                            }
                        }
                        // 保存配置
                        configManager.saveUserId(userId)
                        configManager.saveUserName(userName)
                        configManager.saveApiKey(apiKey)
                        configManager.saveBaseUrl(baseUrl)
                        configManager.saveSelectedModel(selectedModel)
                        configManager.saveImgRecognitionEnabled(imgRecognitionEnabled)

                        if (imgRecognitionEnabled) {
                            configManager.saveImgApiKey(imgApiKey)
                            configManager.saveImgBaseUrl(imgBaseUrl)
                            configManager.saveSelectedImgModel(selectedImgModel)
                        }

                        configManager.saveMaxContextMessageSize(maxContextCount.toIntOrNull() ?:15)
                        configManager.saveSummarizeTriggerCount(summarizeCount.toIntOrNull() ?: 20)
                        configManager.saveMaxSummarizeCount(maxSummarizeCount.toIntOrNull() ?: 20)
                        configManager.saveEnableSeparator(enableSeparator)
                        configManager.saveEnableTimePrefix(enableTimePrefix)

                        // 保存用户头像
                        saveUserAvatar()

                        Toast.makeText(context, "配置保存成功", Toast.LENGTH_SHORT).show()
                        AIChatManager.init()
                        activity.finish()
                    },
                    Modifier
                        .padding(60.dp, 3.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Pink, Gold)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .weight(1f)
                ) {
                    Text("保存", style = MaterialTheme.typography.bodyLarge.copy(color = WhiteText))
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) BlackBg else WhiteBg)
            .padding(16.dp)
            .imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 用户配置
            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "用户ID",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = userId,
                onValueChange = { userId = it },
                placeholder = { Text("目前没啥用") }
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "用户昵称",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = userName,
                onValueChange = { userName = it },
                placeholder = { Text("AI角色对你的称呼") }
            )

            Text(
                text = "用户头像",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSystemInDarkTheme()) DarkGray else LightGray)
                    .clickable { pickUserAvatarFromGallery() }
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                if (userAvatarBitmap.value != null) {
                    Image(
                        bitmap = userAvatarBitmap.value!!.asImageBitmap(),
                        contentDescription = "用户头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // 删除头像按钮
                    IconButton(
                        onClick = {
                            hasNewUserAvatar.value = false
                            userAvatarBitmap.value = null
                        },
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
                } else if (!userAvatarPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(userAvatarPath!!.toUri())
                            .crossfade(true)
                            .build(),
                        contentDescription = "用户头像",
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

            // API配置
            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "API Key",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = apiKey,
                onValueChange = { apiKey = it },
                placeholder = { Text("输入你的API Key") }
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "中转地址",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = baseUrl,
                onValueChange = { baseUrl = it },
                placeholder = { Text("例如: https://vg.xxxx.cc") }
            )

            Text(
                text = "选择模型",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
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
                        .clickable {
                            sheetModels.value = models
                            onModelSelectedCallback.value = { selectedModel = it }
                            setChatModelSheetVisible(true)
                        }
                        .padding(16.dp)) {
                    Text(
                        text = selectedModel.ifEmpty { "请选择模型" },
                        modifier = Modifier.fillMaxWidth(),
                        color = WhiteText
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    if (!isLoadingModels) {
                        Text(
                            text = "加载",
                            modifier = Modifier.clickable {
                                if (apiKey.isEmpty() || baseUrl.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "请先填写API Key和中转地址",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    return@clickable
                                }

                                loadModels(
                                    apiKey = apiKey,
                                    baseUrl = baseUrl,
                                    type = 0,
                                    coroutineScope = coroutineScope,
                                    setLoading = { isLoadingModels = it },
                                    setModels = { models = it },
                                    setSelectedModel = { selectedModel = it },
                                    configManager = configManager
                                )
                            },
                            color = WhiteText
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp),
                            color = WhiteText,
                        )
                    }
                }

            }

            // 图片识别配置
            SwitchWithText(
                checked = imgRecognitionEnabled,
                onCheckedChange = { imgRecognitionEnabled = it },
                text = "开启图片识别",
                modifier = Modifier.padding(top = 16.dp)
            )

            if (imgRecognitionEnabled) {
                SettingTextFieldItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    label = "图片识别API Key",
                    labelPadding = PaddingValues(bottom = 6.dp),
                    value = imgApiKey,
                    onValueChange = { imgApiKey = it },
                    placeholder = { Text("输入你的API Key") }
                )

                SettingTextFieldItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    label = "图片识别中转地址",
                    labelPadding = PaddingValues(bottom = 6.dp),
                    value = imgBaseUrl,
                    onValueChange = { imgBaseUrl = it },
                    placeholder = { Text("例如: https://vg.xxxx.cc") }
                )

                Text(
                    text = "选择图片识别模型(请确保具备图片识别功能)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
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
                            .clickable {
                                sheetModels.value = imgModels
                                onModelSelectedCallback.value = { selectedImgModel = it }
                                setImgModelSheetVisible(true)
                            }
                            .padding(16.dp)) {
                        Text(
                            text = if (selectedImgModel.isNotEmpty()) selectedImgModel else "请选择图片识别模型",
                            modifier = Modifier.fillMaxWidth(),
                            color = WhiteText
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        if (!isLoadingImgModels) {
                            Text(
                                text = "加载",
                                modifier = Modifier.clickable(imgRecognitionEnabled) {
                                    if (!imgRecognitionEnabled) {
                                        Toast.makeText(
                                            context,
                                            "请先开启图片识别开关",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                        return@clickable
                                    }
                                    if (imgApiKey.isEmpty() || imgBaseUrl.isEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "请先填写图片识别API Key和中转地址",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                        return@clickable
                                    }

                                    loadModels(
                                        apiKey = imgApiKey,
                                        baseUrl = imgBaseUrl,
                                        type = 1,
                                        coroutineScope = coroutineScope,
                                        setLoading = { isLoadingImgModels = it },
                                        setModels = { imgModels = it },
                                        setSelectedModel = { selectedImgModel = it },
                                        configManager = configManager
                                    )
                                },
                                color = WhiteText
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp),
                                color = WhiteText,
                            )
                        }
                    }

                }
            }

            // 其他配置
            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "最大上下文条数",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = maxContextCount,
                onValueChange = { maxContextCount = it },
                placeholder = { Text("例如: 10") }
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "触发总结对话数",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = summarizeCount,
                onValueChange = { summarizeCount = it },
                placeholder = { Text("例如: 20") }
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "最大总结次数",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = maxSummarizeCount,
                onValueChange = { maxSummarizeCount = it },
                placeholder = { Text("设定最高总结次数, 避免过高消耗") }
            )
            // 分隔符启用开关
            SwitchWithText(
                checked = enableSeparator,
                onCheckedChange = { enableSeparator = it },
                text = "启用分隔符\"/\"",
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(text = "此功能不再内置，需要自行在提示词中提示AI使用分隔符", style = MaterialTheme.typography.labelSmall.copy(GrayText))

            SwitchWithText(
                checked = enableTimePrefix,
                onCheckedChange = { enableTimePrefix = it },
                text = "消息附带当前时间",
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(text = "此选项不会影响消息内容的显示，但关闭后AI将无法得知当前时间，按需启用", style = MaterialTheme.typography.labelSmall.copy(GrayText))

            Spacer(modifier = Modifier.height(16.dp))
        }
        // 添加模型选择抽屉
        if (isChatModelSheetVisible || isImgModelSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = {
                    setChatModelSheetVisible(false)
                    setImgModelSheetVisible(false)
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
                    items(sheetModels.value.size) { index ->
                        val modelName = sheetModels.value[index].id
                        ListItem(
                            headlineContent = { Text(modelName) },
                            modifier = Modifier.clickable {
                                onModelSelectedCallback.value?.invoke(modelName)
                                setChatModelSheetVisible(false)
                                setImgModelSheetVisible(false)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun loadModels(
    apiKey: String,
    baseUrl: String,
    type: Int,
    coroutineScope: CoroutineScope,
    setLoading: (Boolean) -> Unit,
    setModels: (List<Model>) -> Unit,
    setSelectedModel: (String) -> Unit,
    configManager: ConfigManager
) {
    setLoading(true)

    coroutineScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
        Log.e("ConfigActivity", "IO协程异常", throwable)
        setLoading(false)
    }) {
        val apiService = ApiService(baseUrl, apiKey)
        apiService.getSupportedModels(
            onSuccess = { modelList ->
                Log.d("ConfigActivity", "获取模型列表成功, 模型数量: ${modelList.size}")
                try {
                    // 直接在当前线程更新数据，然后启动Main线程更新UI
                    setModels(modelList)

                    // 设置默认选中项
                    var selectedId: String
                    if (type == 0) {
                        val savedModel = configManager.getSelectedModel()
                        if (savedModel != null) {
                            val position = modelList.indexOfFirst { it.id == savedModel }
                            selectedId = if (position != -1) {
                                Log.d("ConfigActivity", "找到保存的模型: ${modelList[position].id}")
                                modelList[position].id
                            } else {
                                // 未找到保存的模型，选中第一个
                                Log.d(
                                    "ConfigActivity",
                                    "未找到保存的模型，选中第一个: ${modelList.firstOrNull()?.id}"
                                )
                                modelList.firstOrNull()?.id ?: ""
                            }
                        } else {
                            // 没有保存的模型，选中第一个
                            Log.d(
                                "ConfigActivity",
                                "没有保存的模型，选中第一个: ${modelList.firstOrNull()?.id}"
                            )
                            selectedId = modelList.firstOrNull()?.id ?: ""
                        }
                    } else {
                        val savedImgModel = configManager.getSelectedImgModel()
                        if (savedImgModel != null) {
                            val position = modelList.indexOfFirst { it.id == savedImgModel }
                            selectedId = if (position != -1) {
                                Log.d(
                                    "ConfigActivity",
                                    "找到保存的图片识别模型: ${modelList[position].id}"
                                )
                                modelList[position].id
                            } else {
                                // 未找到保存的模型，选中第一个
                                Log.d(
                                    "ConfigActivity",
                                    "未找到保存的图片识别模型，选中第一个: ${modelList.firstOrNull()?.id}"
                                )
                                modelList.firstOrNull()?.id ?: ""
                            }
                        } else {
                            // 没有保存的模型，选中第一个
                            Log.d(
                                "ConfigActivity",
                                "没有保存的图片识别模型，选中第一个: ${modelList.firstOrNull()?.id}"
                            )
                            selectedId = modelList.firstOrNull()?.id ?: ""
                        }
                    }


                    // 在主线程中执行UI更新操作
                    CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                        try {
                            Log.d("ConfigActivity", "设置默认选中项: $selectedId")
                            if (selectedId.isNotEmpty()) {
                                setSelectedModel(selectedId)
                            }
                            setLoading(false)
                        } catch (e: Exception) {
                            Log.e("ConfigActivity", "设置模型列表或选中项时出错", e)
                            setLoading(false)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ConfigActivity", "处理模型数据时出错", e)
                    // 确保无论如何都停止加载状态
                    CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                        setLoading(false)
                    }
                }
            },
            onError = { errorMsg ->
                Log.e("ConfigActivity", "加载模型失败: $errorMsg")
                // 确保在主线程中更新UI
                CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                    setLoading(false)
                }
            }
        )
    }
}