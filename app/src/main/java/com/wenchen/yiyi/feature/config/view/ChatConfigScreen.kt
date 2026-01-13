package com.wenchen.yiyi.feature.config.view

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wenchen.yiyi.core.datastore.storage.ImageManager
import com.wenchen.yiyi.core.model.network.Model
import com.wenchen.yiyi.core.common.theme.BlackBg
import com.wenchen.yiyi.core.common.theme.Gold
import com.wenchen.yiyi.core.common.theme.Pink
import com.wenchen.yiyi.core.common.theme.WhiteBg
import com.wenchen.yiyi.core.common.theme.WhiteText
import com.wenchen.yiyi.core.util.StatusBarUtil
import com.wenchen.yiyi.core.designSystem.component.SettingTextFieldItem
import com.wenchen.yiyi.core.common.theme.BlackText
import com.wenchen.yiyi.core.common.theme.DarkGray
import com.wenchen.yiyi.core.common.theme.GrayText
import com.wenchen.yiyi.core.common.theme.LightGray
import com.wenchen.yiyi.core.model.config.ApiConfig
import com.wenchen.yiyi.core.model.config.UserConfig
import com.wenchen.yiyi.core.designSystem.component.SwitchWithText
import com.wenchen.yiyi.feature.config.viewmodel.ChatConfigViewModel
import java.io.File

/**
 * 配置页面路由
 *
 * @param viewModel 配置页面 ViewModel
 */
@Composable
internal fun ChatConfigRoute(
    viewModel: ChatConfigViewModel = hiltViewModel(),
    navController: NavController
) {
    ChatConfigScreen(
        viewModel = viewModel,
        navController = navController
    )
}

@Composable
fun ChatConfigScreen(
    viewModel: ChatConfigViewModel = hiltViewModel(),
    navController: NavController = NavController(LocalContext.current)
) {
    ChatConfigScreenContent(
        viewModel = viewModel,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConfigScreenContent(
    viewModel : ChatConfigViewModel,
    navController: NavController
){
    val activity = LocalActivity.current
    if (isSystemInDarkTheme()) {
        StatusBarUtil.setStatusBarTextColor(activity as ComponentActivity, false)
    } else {
        StatusBarUtil.setStatusBarTextColor(activity as ComponentActivity, true)
    }
    val context = LocalContext.current

    var userId by remember { mutableStateOf(viewModel.userConfig?.userId ?: "123123") }
    var userName by remember { mutableStateOf(viewModel.userConfig?.userName ?: "温辰") }

    // API配置列表状态
    var apiConfigs by remember { mutableStateOf(viewModel.userConfig?.apiConfigs ?: emptyList()) }
    var currentApiConfigId by remember { mutableStateOf(viewModel.userConfig?.currentApiConfigId) }
    var showAddApiDialog by remember { mutableStateOf(false) }

    // 对话模型配置
    var apiKey by remember { mutableStateOf(viewModel.userConfig?.baseApiKey ?: "") }
    var baseUrl by remember { mutableStateOf(viewModel.userConfig?.baseUrl ?: "") }
    var selectedModel by remember { mutableStateOf(viewModel.userConfig?.selectedModel ?: "") }
    var isLoadingModels by remember { mutableStateOf(false) }

    // 图片识别配置
    var imgRecognitionEnabled by remember { mutableStateOf(viewModel.userConfig?.imgRecognitionEnabled ?: false) }
    var imgApiConfigs by remember { mutableStateOf(viewModel.userConfig?.apiConfigs ?: emptyList()) }
    var currentImgApiConfigId by remember { mutableStateOf(viewModel.userConfig?.currentApiConfigId) }
    var showAddImgApiDialog by remember { mutableStateOf(false) }
    var imgApiKey by remember { mutableStateOf(viewModel.userConfig?.imgApiKey ?: "") }
    var imgBaseUrl by remember { mutableStateOf(viewModel.userConfig?.imgBaseUrl ?: "") }
    var selectedImgModel by remember { mutableStateOf(viewModel.userConfig?.selectedImgModel ?: "") }
    var isLoadingImgModels by remember { mutableStateOf(false) }

    // 其他配置
    var maxContextCount by remember { mutableStateOf(viewModel.userConfig?.maxContextMessageSize?.toString() ?: "10") }
    var summarizeCount by remember { mutableStateOf(viewModel.userConfig?.summarizeTriggerCount?.toString() ?: "20") }
    var maxSummarizeCount by remember { mutableStateOf(viewModel.userConfig?.maxSummarizeCount?.toString() ?: "20") }
    var enableSeparator by remember { mutableStateOf(viewModel.userConfig?.enableSeparator ?: false) }
    var enableTimePrefix by remember { mutableStateOf(viewModel.userConfig?.enableTimePrefix ?: true) }
    var enableStreamOutput by remember { mutableStateOf(viewModel.userConfig?.enableStreamOutput ?: true) }

    // 开发者设置
    var showLogcatView by remember { mutableStateOf(viewModel.userConfig?.showLogcatView ?: false) }

    // 用户头像相关
    var userAvatarPath by remember { mutableStateOf(viewModel.userConfig?.userAvatarPath) }
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
            val oldAvatarPath = viewModel.userConfig?.userAvatarPath
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
            val userId = viewModel.userConfig?.userId ?: "123123"
            val savedFile = imageManager.saveAvatarImage("user_$userId", userAvatarBitmap.value!!)

            if (savedFile != null) {
                userAvatarPath = savedFile.toUri().toString()
            }
        }
    }

    LaunchedEffect(Unit) {
        // 自动加载对话模型
        if (apiKey.isNotEmpty() && baseUrl.isNotEmpty()) {
            viewModel.executeGetModels(
                apiKey = apiKey,
                baseUrl = baseUrl,
                type = 0,
                setLoading = { isLoadingModels = it },
                setSelectedModel = { selectedModel = it }
            )
        }

        // 如果图片识别功能已启用，则自动加载图片识别模型
        if (imgRecognitionEnabled && imgApiKey.isNotEmpty() && imgBaseUrl.isNotEmpty()) {
            viewModel.executeGetModels(
                apiKey = imgApiKey,
                baseUrl = imgBaseUrl,
                type = 1,
                setLoading = { isLoadingImgModels = it },
                setSelectedModel = { selectedImgModel = it }
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
                        .clickable { viewModel.navigateBack() }
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

                        if (viewModel.models.value.isEmpty() || selectedModel.isEmpty()) {
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

                            if (viewModel.imgModels.value.isEmpty() || selectedImgModel.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "请先加载并选择图片识别模型",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                return@TextButton
                            }
                        }
                        // 保存用户头像
                        saveUserAvatar()
                        // 保存配置
                        val userConfig = UserConfig(
                            userId = userId,
                            userName = userName,
                            userAvatarPath = userAvatarPath,
                            apiConfigs = apiConfigs,
                            currentApiConfigId = currentApiConfigId,
//                            baseApiKey = apiKey,
//                            baseUrl = baseUrl,
                            selectedModel = selectedModel,
                            imgRecognitionEnabled = imgRecognitionEnabled,
//                            imgApiKey = imgApiKey,
//                            imgBaseUrl = imgBaseUrl,
                            selectedImgModel = selectedImgModel,
                            maxContextMessageSize = maxContextCount.toIntOrNull() ?: 15,
                            summarizeTriggerCount = summarizeCount.toIntOrNull() ?: 20,
                            maxSummarizeCount = maxSummarizeCount.toIntOrNull() ?: 20,
                            enableSeparator = enableSeparator,
                            enableTimePrefix = enableTimePrefix,
                            enableStreamOutput = enableStreamOutput,
                            showLogcatView = showLogcatView
                        )
                        viewModel.updateUserConfig(userConfig)
                        Toast.makeText(context, "配置保存成功", Toast.LENGTH_SHORT).show()
                        viewModel.navigateBack()
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // API 配置选择与添加
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "API 配置列表", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showAddApiDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加配置")
                }
            }

            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                val currentConfig = apiConfigs.find { it.id == currentApiConfigId }
                OutlinedCard(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentConfig?.name ?: "未选择配置",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    apiConfigs.forEach { config ->
                        DropdownMenuItem(
                            text = { Text(config.name) },
                            onClick = {
                                currentApiConfigId = config.id
                                apiKey = config.apiKey ?: ""
                                baseUrl = config.baseUrl ?: ""
                                selectedModel = config.selectedModel ?: ""
                                expanded = false
                            }
                        )
                    }
                    if (apiConfigs.isEmpty()) {
                        DropdownMenuItem(text = { Text("暂无配置") }, onClick = { expanded = false })
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
                placeholder = { Text("例如: https://xx.xxxx.cc") }
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
                            sheetModels.value = viewModel.models.value
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

                                viewModel.executeGetModels(
                                    apiKey = apiKey,
                                    baseUrl = baseUrl,
                                    type = 0,
                                    setLoading = { isLoadingModels = it },
                                    setSelectedModel = { selectedModel = it }
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // API 配置选择与添加
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "图片识别 API 配置列表", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showAddImgApiDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("添加配置")
                    }
                }

                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    val currentConfig = imgApiConfigs.find { it.id == currentImgApiConfigId }
                    OutlinedCard(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentConfig?.name ?: "未选择配置",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        imgApiConfigs.forEach { config ->
                            DropdownMenuItem(
                                text = { Text(config.name) },
                                onClick = {
                                    currentImgApiConfigId = config.id
                                    imgApiKey = config.apiKey ?: ""
                                    imgBaseUrl = config.baseUrl ?: ""
                                    selectedImgModel = config.selectedModel ?: ""
                                    expanded = false
                                }
                            )
                        }
                        if (imgApiConfigs.isEmpty()) {
                            DropdownMenuItem(text = { Text("暂无配置") }, onClick = { expanded = false })
                        }
                    }
                }

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
                    placeholder = { Text("例如: https://xx.xxxx.cc") }
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
                                sheetModels.value = viewModel.imgModels.value
                                onModelSelectedCallback.value = { selectedImgModel = it }
                                setImgModelSheetVisible(true)
                            }
                            .padding(16.dp)) {
                        Text(
                            text = selectedImgModel.ifEmpty { "请选择图片识别模型" },
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

                                    viewModel.executeGetModels(
                                        apiKey = imgApiKey,
                                        baseUrl = imgBaseUrl,
                                        type = 1,
                                        setLoading = { isLoadingImgModels = it },
                                        setSelectedModel = { selectedImgModel = it }
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

            SwitchWithText(
                checked = enableStreamOutput,
                onCheckedChange = { enableStreamOutput = it },
                text = "启用流式输出",
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(text = "开启后，AI的回复将实时显示在屏幕上\n若所选模型不支持流式输出，此选项将失效，必要时请关闭", style = MaterialTheme.typography.labelSmall.copy(GrayText))

            // 开发者设置
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text("开发者设置", style = MaterialTheme.typography.titleMedium)
            SwitchWithText(
                checked = showLogcatView,
                onCheckedChange = { showLogcatView = it },
                text = "显示悬浮日志",
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 添加 API 配置对话框
        if (showAddApiDialog || showAddImgApiDialog) {
            AddApiConfigDialog(
                onDismiss = { showAddApiDialog = false; showAddImgApiDialog = false },
                onSave = { name, url, key ->
                    val newConfig = ApiConfig(
                        name = name.ifBlank { url },
                        baseUrl = url,
                        apiKey = key
                    )
                    if (showAddApiDialog) {
                        apiConfigs = apiConfigs + newConfig
                        currentApiConfigId = newConfig.id
                        apiKey = key
                        baseUrl = url
                        showAddApiDialog = false
                    } else if (showAddImgApiDialog) {
                        imgApiConfigs = imgApiConfigs + newConfig
                        currentImgApiConfigId = newConfig.id
                        imgApiKey = key
                        imgBaseUrl = url
                        showAddImgApiDialog = false
                    }
                }
            )
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

@Composable
fun AddApiConfigDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加 API 配置") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("配置名称 (可选)") },
                    placeholder = { Text("默认为 URL 地址") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("中转地址 (必填)") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API Key (必填)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, url, key) },
                enabled = url.isNotBlank() && key.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
