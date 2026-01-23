package com.wenchen.yiyi.feature.config.view

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.wenchen.yiyi.core.designSystem.component.SpaceVerticalSmall
import com.wenchen.yiyi.core.designSystem.theme.*
import com.wenchen.yiyi.core.ui.SettingTextFieldItem
import com.wenchen.yiyi.core.ui.SwitchWithText
import com.wenchen.yiyi.core.model.config.ApiConfig
import com.wenchen.yiyi.core.model.network.Model
import com.wenchen.yiyi.core.ui.BottomGradientButton
import com.wenchen.yiyi.feature.config.viewmodel.ApiConfigViewModel

@Composable
internal fun ApiConfigRoute(
    viewModel: ApiConfigViewModel = hiltViewModel(),
    navController: NavController
) {
    ApiConfigScreen(
        viewModel = viewModel,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigScreen(
    viewModel: ApiConfigViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val userConfig = viewModel.userConfig
    val uiState = viewModel.apiUIState.collectAsState().value


    // API配置列表状态
    var apiConfigs by remember { mutableStateOf(userConfig?.apiConfigs ?: emptyList()) }
    var currentApiConfigId by remember { mutableStateOf(userConfig?.currentApiConfigId) }
    var showAddApiDialog by remember { mutableStateOf(false) }
    var editingApiConfig by remember { mutableStateOf<ApiConfig?>(null) }

    // 对话模型配置
    var apiKey by remember { mutableStateOf(userConfig?.baseApiKey ?: "") }
    var baseUrl by remember { mutableStateOf(userConfig?.baseUrl ?: "") }
    var selectedModel by remember { mutableStateOf(userConfig?.selectedModel ?: "") }

    // 图片识别配置
    var imgRecognitionEnabled by remember {
        mutableStateOf(
            userConfig?.imgRecognitionEnabled ?: false
        )
    }
    var currentImgApiConfigId by remember { mutableStateOf(userConfig?.currentImgApiConfigId) }
    var imgApiKey by remember { mutableStateOf(userConfig?.imgApiKey ?: "") }
    var imgBaseUrl by remember { mutableStateOf(userConfig?.imgBaseUrl ?: "") }
    var selectedImgModel by remember { mutableStateOf(userConfig?.selectedImgModel ?: "") }

    val (isChatModelSheetVisible, setChatModelSheetVisible) = remember { mutableStateOf(false) }
    val (isImgModelSheetVisible, setImgModelSheetVisible) = remember { mutableStateOf(false) }
    val sheetModels = remember { mutableStateOf<List<Model>>(emptyList()) }
    val onModelSelectedCallback = remember { mutableStateOf<((String) -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        if (apiKey.isNotEmpty() && baseUrl.isNotEmpty()) {
            viewModel.executeGetModels(apiKey, baseUrl, 0) { selectedModel = it }
        }
        if (imgRecognitionEnabled && imgApiKey.isNotEmpty() && imgBaseUrl.isNotEmpty()) {
            viewModel.executeGetModels(imgApiKey, imgBaseUrl, 1) { selectedImgModel = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 配置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "返回",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomGradientButton(
                onClick = {
                    if (apiKey.isEmpty() || baseUrl.isEmpty()) {
                        Toast.makeText(context, "请填写完整的API Key和中转地址", Toast.LENGTH_SHORT)
                            .show()
                        return@BottomGradientButton
                    }
                    viewModel.updateUserConfig(
                        apiConfigs,
                        currentApiConfigId,
                        selectedModel,
                        imgRecognitionEnabled,
                        currentImgApiConfigId,
                        selectedImgModel
                    )
                    Toast.makeText(context, "保存API配置", Toast.LENGTH_SHORT).show()
                },
                text = "保存"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // API 配置列表
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "API 配置列表", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    editingApiConfig = null
                    showAddApiDialog = true
                }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("添加")
                }
            }

            var expanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
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
                            modifier = Modifier.weight(1f)
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
                            },
                            trailingIcon = {
                                Row {
                                    TextButton(onClick = {
                                        editingApiConfig = config; showAddApiDialog =
                                        true; expanded = false
                                    }) { Text("编辑") }
                                    TextButton(onClick = {
                                        val newList = apiConfigs.filter { it.id != config.id }
                                        apiConfigs = newList
                                        if (currentApiConfigId == config.id) {
                                            val next = newList.firstOrNull()
                                            currentApiConfigId = next?.id
                                            apiKey = next?.apiKey ?: ""
                                            baseUrl = next?.baseUrl ?: ""
                                            selectedModel = next?.selectedModel ?: ""
                                        }
                                    }) { Text("删除") }
                                }
                            }
                        )
                    }
                }
            }

            SettingTextFieldItem(
                modifier = Modifier.fillMaxWidth(),
                enable = false,
                label = "API Key",
                value = apiKey,
                singleLine = true,
                onValueChange = {})
            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enable = false,
                label = "中转地址",
                value = baseUrl,
                singleLine = true,
                onValueChange = {})

            Text(
                text = "选择模型",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            ModelSelector(
                selectedModel = selectedModel,
                isLoading = uiState.isLoadingModels,
                onSelectClick = {
                    sheetModels.value = viewModel.models.value
                    onModelSelectedCallback.value = { selectedModel = it }
                    setChatModelSheetVisible(true)
                },
                onLoadClick = {
                    if (apiKey.isNotBlank() && baseUrl.isNotBlank()) {
                        viewModel.executeGetModels(apiKey, baseUrl, 0) { selectedModel = it }
                    } else {
                        Toast.makeText(context, "请先填写API Key和中转地址", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SwitchWithText(
                checked = imgRecognitionEnabled,
                onCheckedChange = { imgRecognitionEnabled = it },
                text = "开启图片识别"
            )

            if (imgRecognitionEnabled) {
                SpaceVerticalSmall()
                // Similar UI for Image API
                var expandedImg by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    val currentConfig = apiConfigs.find { it.id == currentImgApiConfigId }
                    OutlinedCard(
                        onClick = { expandedImg = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentConfig?.name ?: "未选择配置",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedImg,
                        onDismissRequest = { expandedImg = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        apiConfigs.forEach { config ->
                            DropdownMenuItem(
                                text = { Text(config.name) },
                                onClick = {
                                    currentImgApiConfigId = config.id
                                    imgApiKey = config.apiKey ?: ""
                                    imgBaseUrl = config.baseUrl ?: ""
                                    selectedImgModel = config.selectedModel ?: ""
                                    expandedImg = false
                                }
                            )
                        }
                    }
                }
                SettingTextFieldItem(
                    modifier = Modifier.fillMaxWidth(),
                    enable = false,
                    label = "图片识别 API Key",
                    value = imgApiKey,
                    singleLine = true,
                    onValueChange = {})
                SettingTextFieldItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    enable = false,
                    label = "图片识别中转地址",
                    value = imgBaseUrl,
                    singleLine = true,
                    onValueChange = {})

                Text(
                    text = "选择图片识别模型",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                ModelSelector(
                    selectedModel = selectedImgModel,
                    isLoading = uiState.isLoadingImgModels,
                    onSelectClick = {
                        sheetModels.value = viewModel.imgModels.value
                        onModelSelectedCallback.value = { selectedImgModel = it }
                        setImgModelSheetVisible(true)
                    },
                    onLoadClick = {
                        if (imgApiKey.isNotBlank() && imgBaseUrl.isNotBlank()) {
                            viewModel.executeGetModels(
                                imgApiKey,
                                imgBaseUrl,
                                1
                            ) { selectedImgModel = it }
                        } else {
                            Toast.makeText(
                                context,
                                "请先填写图片识别API Key和中转地址",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }

        if (showAddApiDialog) {
            AddApiConfigDialog(
                config = editingApiConfig,
                onDismiss = { showAddApiDialog = false; editingApiConfig = null },
                onSave = { name, url, key ->
                    if (editingApiConfig != null) {
                        apiConfigs = apiConfigs.map {
                            if (it.id == editingApiConfig?.id) it.copy(
                                name = name.ifBlank { url },
                                baseUrl = url,
                                apiKey = key
                            ) else it
                        }
                        if (currentApiConfigId == editingApiConfig?.id) {
                            apiKey = key; baseUrl = url
                        }
                        editingApiConfig = null
                    } else {
                        val newConfig =
                            ApiConfig(name = name.ifBlank { url }, baseUrl = url, apiKey = key)
                        apiConfigs = apiConfigs + newConfig
                        currentApiConfigId = newConfig.id
                        apiKey = key; baseUrl = url
                    }
                    showAddApiDialog = false
                }
            )
        }

        if (isChatModelSheetVisible || isImgModelSheetVisible) {
            ModelBottomSheet(
                models = sheetModels.value,
                onDismiss = { setChatModelSheetVisible(false); setImgModelSheetVisible(false) },
                onModelSelected = {
                    onModelSelectedCallback.value?.invoke(it)
                    setChatModelSheetVisible(false)
                    setImgModelSheetVisible(false)
                }
            )
        }
    }
}

@Composable
fun ModelSelector(
    selectedModel: String,
    isLoading: Boolean,
    onSelectClick: () -> Unit,
    onLoadClick: () -> Unit
) {
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
                .clickable { onSelectClick() }
                .padding(16.dp)) {
            Text(text = selectedModel.ifEmpty { "请选择模型" }, color = TextWhite)
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(12.dp)) {
            if (!isLoading) {
                Text(
                    text = "加载",
                    modifier = Modifier.clickable { onLoadClick() },
                    color = TextWhite
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextWhite)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelBottomSheet(
    models: List<Model>,
    onDismiss: () -> Unit,
    onModelSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        val screenHeight =
            with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight * 0.6f)
        ) {
            items(models.size) { index ->
                val modelName = models[index].id
                ListItem(
                    headlineContent = { Text(modelName) },
                    modifier = Modifier.clickable { onModelSelected(modelName) })
            }
        }
    }
}

@Composable
fun AddApiConfigDialog(
    config: ApiConfig? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(config?.name ?: "") }
    var url by remember { mutableStateOf(config?.baseUrl ?: "") }
    var key by remember { mutableStateOf(config?.apiKey ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (config == null) "添加 API 配置" else "编辑 API 配置") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("配置名称 (可选)") },
                    placeholder = { Text("默认为 URL 地址") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("中转地址 (必填)") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
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
