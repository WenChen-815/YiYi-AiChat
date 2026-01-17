package com.wenchen.yiyi

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.designSystem.component.UpdateAnnouncementDialog
import com.wenchen.yiyi.core.log.FloatingLogcatView
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.util.PermissionUtils
import com.wenchen.yiyi.core.util.ui.StatusBarUtils
import com.wenchen.yiyi.core.util.system.WebViewPool
import com.wenchen.yiyi.core.util.storage.MMKVUtils
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navigator: AppNavigator

    @Inject
    lateinit var userConfigState: UserConfigState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebViewPool.prepare(this)
        // 设置状态栏
        StatusBarUtils.transparentNavBar(this)

        // 请求权限
        PermissionUtils.checkAndRequestStoragePermission(this, requestPermissionLauncher)
        // 请求悬浮窗权限
        PermissionUtils.checkAndRequestOverlayPermission(this, overlayPermissionLauncher)

        setContent {
            AIChatTheme {
                val userConfig by userConfigState.userConfig.collectAsState()
                val showLogcatView by mutableStateOf(userConfig?.showLogcatView ?: false)

                AppNavHost(navigator = navigator)
                if (showLogcatView) {
                    FloatingLogcatView()
                }
                val currentVersion = BuildConfig.VERSION_NAME
                val announcementKey = "show_update_announcement_$currentVersion"
                var showUpdateDialog by remember {
                    // 读取 MMKV，如果没有记录则显示（默认 false）
                    mutableStateOf(!MMKVUtils.getBoolean(announcementKey, false))
                }

                if (showUpdateDialog) {
                    UpdateAnnouncementDialog(
                        versionName = currentVersion,
                        announcement = """
                            1. 新增了API分组切换功能。由于存储方式变更，可能出现原配置丢失问题，请动动手指重新配置即可。
                            2. 新增了更新公告弹窗功能（就像这样）
                            3. 修复了聊天界面的一些漏洞
                        """.trimIndent(),
                        onDismiss = { showUpdateDialog = false }
                    )
                }
            }
        }
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
