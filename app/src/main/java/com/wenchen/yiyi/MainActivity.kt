package com.wenchen.yiyi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.hjq.permissions.permission.PermissionLists
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.designSystem.component.UpdateAnnouncementDialog
import com.wenchen.yiyi.core.log.FloatingLogcatView
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.util.storage.MMKVUtils
import com.wenchen.yiyi.core.util.system.PermissionUtils
import com.wenchen.yiyi.core.util.system.WebViewPool
import com.wenchen.yiyi.core.util.ui.StatusBarUtils
import com.wenchen.yiyi.navigation.AppNavHost
import com.wenchen.yiyi.navigation.AppNavigator
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

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            // targetSDK < 33 可以申请存储权限
            PermissionUtils.requestStoragePermission(this) { granted ->
                if (granted) {
                    Timber.tag("MainActivity").d("存储权限已授予")
                }
            }
        }
        // 请求悬浮窗权限
        PermissionUtils.requestCustomPermissions(
            this,
            arrayOf(PermissionLists.getSystemAlertWindowPermission()),
            "悬浮窗权限"
        ) { granted ->
            if (granted) {
                Timber.tag("MainActivity").d("悬浮窗权限已授予")
            }
        }

        setContent {
            AIChatTheme {
                val userConfig by userConfigState.userConfig.collectAsState()

                AppNavHost(navigator = navigator)
                if (userConfig?.showLogcatView ?: false) {
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
}
