package com.wenchen.yiyi.common

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wenchen.yiyi.aiChat.ui.activity.CharacterEditActivity
import com.wenchen.yiyi.aiChat.common.AIChatManager
import com.wenchen.yiyi.config.common.ConfigManager
import com.wenchen.yiyi.common.theme.AIChatTheme
import com.wenchen.yiyi.common.theme.Gold
import com.wenchen.yiyi.common.theme.GrayText
import com.wenchen.yiyi.common.theme.Pink
import com.wenchen.yiyi.common.theme.WhiteText
import com.wenchen.yiyi.common.utils.StatusBarUtil
import com.wenchen.yiyi.config.ui.activity.ConfigActivity
import com.wenchen.yiyi.home.ui.HomeScreen
import com.wenchen.yiyi.profile.ui.ProfileScreen
import kotlin.jvm.java

class MainActivity : ComponentActivity() {
    private lateinit var configManager: ConfigManager
    private var currentPosition by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置状态栏
        StatusBarUtil.transparentNavBar(this)

        // 初始化AI聊天管理器
        AIChatManager.init()

        // 请求图片权限
        requestImagePermissionOnStartup()

        setContent {
            AIChatTheme {
                MainScreen()
            }
        }

        // 初始化配置管理器
        configManager = ConfigManager()
        // 检查是否有配置，如果没有则跳转到配置页面
        if (!configManager.hasCompleteConfig()) {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        if (isSystemInDarkTheme()) {
            StatusBarUtil.setStatusBarTextColor(this, false)
        } else {
            StatusBarUtil.setStatusBarTextColor(this, true)
        }
        Scaffold(
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier
//                        .background(MaterialTheme.colorScheme.primary) // 这样设置是无效的，应该用containerColor
                        .fillMaxWidth()
                        .height(66.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 首页导航项
                        TextButton(
                            onClick = { currentPosition = 0 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "首页",
                                color = if (currentPosition == 0) MaterialTheme.colorScheme.onBackground else GrayText
                            )
                        }

                        // 中间发布按钮
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新增角色",
                            tint = WhiteText,
                            modifier = Modifier
                                .size(48.dp, 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Pink, Gold)
                                    )
                                )
                                .clickable {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            CharacterEditActivity::class.java
                                        )
                                    )
                                }
                        )


                        // 我的导航项
                        TextButton(
                            onClick = { currentPosition = 1 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "我的",
                                color = if (currentPosition == 1) MaterialTheme.colorScheme.onBackground else GrayText
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                when (currentPosition) {
                    0 -> HomeScreen(this@MainActivity)
                    1 -> ProfileScreen(this@MainActivity)
                }
            }
        }
    }

    // 在进入界面时请求权限的方法
    private fun requestImagePermissionOnStartup() {
        // 检查Android版本，适配不同的权限模型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14及以上版本 - 使用新的精选照片API，无需提前请求权限
            Log.d("ImgTestActivity", "Android 14+，使用精选照片API无需提前请求权限")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) - 使用READ_MEDIA_IMAGES权限
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Android 12及以下版本 - 使用READ_EXTERNAL_STORAGE权限
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("ImgTestActivity", "图片访问权限已授予")
        } else {
            // 权限被拒绝，显示提示信息
            Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
        }
    }
}
