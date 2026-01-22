package com.wenchen.yiyi.feature.config.view

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.graphics.Brush
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
import com.wenchen.yiyi.core.util.ui.StatusBarUtils
import com.wenchen.yiyi.core.designSystem.component.SettingTextFieldItem
import com.wenchen.yiyi.core.designSystem.component.SwitchWithText
import com.wenchen.yiyi.feature.config.viewmodel.ChatConfigViewModel

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
    viewModel: ChatConfigViewModel,
    navController: NavController
) {
    ChatConfigScreenContent(
        viewModel = viewModel,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConfigScreenContent(
    viewModel: ChatConfigViewModel,
    navController: NavController
) {
    val activity = LocalActivity.current
    if (isSystemInDarkTheme()) {
        StatusBarUtils.setStatusBarTextColor(activity as ComponentActivity, false)
    } else {
        StatusBarUtils.setStatusBarTextColor(activity as ComponentActivity, true)
    }
    val context = LocalContext.current

    var userId by remember { mutableStateOf(viewModel.userConfig?.userId ?: "123123") }
    var userName by remember { mutableStateOf(viewModel.userConfig?.userName ?: "温辰") }

    // 其他配置
    var maxContextCount by remember {
        mutableStateOf(
            viewModel.userConfig?.maxContextMessageSize?.toString() ?: "10"
        )
    }
    var summarizeCount by remember {
        mutableStateOf(
            viewModel.userConfig?.summarizeTriggerCount?.toString() ?: "20"
        )
    }
    var maxSummarizeCount by remember {
        mutableStateOf(
            viewModel.userConfig?.maxSummarizeCount?.toString() ?: "20"
        )
    }
    var enableSeparator by remember {
        mutableStateOf(
            viewModel.userConfig?.enableSeparator ?: false
        )
    }
    var enableTimePrefix by remember {
        mutableStateOf(
            viewModel.userConfig?.enableTimePrefix ?: true
        )
    }
    var enableStreamOutput by remember {
        mutableStateOf(
            viewModel.userConfig?.enableStreamOutput ?: true
        )
    }

    // 开发者设置
    var showLogcatView by remember { mutableStateOf(viewModel.userConfig?.showLogcatView ?: false) }

    // 用户头像相关
    var userAvatarPath by remember { mutableStateOf(viewModel.userConfig?.userAvatarPath) }
    val userAvatarBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val hasNewUserAvatar = remember { mutableStateOf(false) }

    val pickUserAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                            activity.contentResolver,
                            imageUri!!
                        )
                    )
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("聊天配置") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "返回",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        val finalAvatarPath =
                            viewModel.saveUserAvatar(hasNewUserAvatar.value, userAvatarBitmap.value)
                        viewModel.updateUserConfig(
                            userId = userId,
                            userName = userName,
                            userAvatarPath = finalAvatarPath,
                            maxContextCount = maxContextCount,
                            summarizeCount = summarizeCount,
                            maxSummarizeCount = maxSummarizeCount,
                            enableSeparator = enableSeparator,
                            enableTimePrefix = enableTimePrefix,
                            enableStreamOutput = enableStreamOutput,
                            showLogcatView = showLogcatView
                        )
                        Toast.makeText(context, "配置保存成功", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            brush = Brush.horizontalGradient(colors = listOf(Pink, Gold)),
                            shape = RoundedCornerShape(25.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                ) {
                    Text("保存", style = MaterialTheme.typography.bodyLarge.copy(color = WhiteText))
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) BlackBg else WhiteBg)
            .imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
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
                placeholder = { Text("用户唯一标识") }
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
                    .size(100.dp)
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
                    IconButton(
                        onClick = {
                            hasNewUserAvatar.value = false
                            userAvatarBitmap.value = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RemoveCircleOutline,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (!userAvatarPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(userAvatarPath!!.toUri()).crossfade(true).build(),
                        contentDescription = "用户头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "添加头像")
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
                placeholder = { Text("10") }
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
                placeholder = { Text("20") }
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
                placeholder = { Text("20") }
            )

            SwitchWithText(
                checked = enableSeparator,
                onCheckedChange = { enableSeparator = it },
                text = "启用分隔符\"/\"",
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "此功能不再内置，需要自行在提示词中提示AI使用分隔符",
                style = MaterialTheme.typography.labelSmall.copy(GrayText)
            )

            SwitchWithText(
                checked = enableTimePrefix,
                onCheckedChange = { enableTimePrefix = it },
                text = "消息附带当前时间",
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "关闭后AI将无法得知当前时间，按需启用",
                style = MaterialTheme.typography.labelSmall.copy(GrayText)
            )

            SwitchWithText(
                checked = enableStreamOutput,
                onCheckedChange = { enableStreamOutput = it },
                text = "启用流式输出",
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "开启后回复将实时显示。若模型不支持则此选项无效",
                style = MaterialTheme.typography.labelSmall.copy(GrayText)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text("开发者设置", style = MaterialTheme.typography.titleMedium)
            SwitchWithText(
                checked = showLogcatView,
                onCheckedChange = { showLogcatView = it },
                text = "显示悬浮日志",
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
