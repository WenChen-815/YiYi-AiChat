package com.wenchen.yiyi

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.wenchen.yiyi.feature.aiChat.common.AIChatManager
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.common.utils.PermissionUtils
import com.wenchen.yiyi.core.common.utils.StatusBarUtil
import com.wenchen.yiyi.feature.config.common.ConfigManager
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navigator: AppNavigator
    private lateinit var configManager: ConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置状态栏
        StatusBarUtil.transparentNavBar(this)

        // 初始化AI聊天管理器
        AIChatManager.init()

        // 请求权限
        PermissionUtils.checkAndRequestStoragePermission(this, requestPermissionLauncher)
        // 请求悬浮窗权限
        PermissionUtils.checkAndRequestOverlayPermission(this, overlayPermissionLauncher)

        setContent {
            AIChatTheme {
//                MainScreen()
                AppNavHost(navigator = navigator)
            }
        }

        // 初始化配置管理器
//        configManager = ConfigManager()
        // 检查是否有配置，如果没有则跳转到配置页面
//        if (!configManager.hasCompleteConfig()) {
//            startActivity(Intent(this, ConfigActivity::class.java))
//        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                Timber.tag("MainActivity").d("权限已授予")
            } else {
                // 权限被拒绝，显示提示信息
                Toast.makeText(this, "需要权限才能正常使用", Toast.LENGTH_SHORT).show()
            }
        }

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                Timber.tag("MainActivity").d("悬浮窗权限已授予")
            } else {
                Toast.makeText(this, "悬浮窗权限未授予，部分功能可能受限", Toast.LENGTH_SHORT).show()
            }
        }
}
