package com.wenchen.yiyi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.hjq.permissions.permission.PermissionLists
import com.wenchen.yiyi.core.designSystem.theme.AppTheme
import com.wenchen.yiyi.core.ui.UpdateAnnouncementDialog
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
            AppTheme {
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
                            🎉必看重大更新🎉
                            
                            -- 修改内容 --
                            1. 角色管理不再对提示词进行拆分，新版本将自动识别并合并角色卡中的提示词
                            2. 移除了旧的世界功能
                            3. 记忆功能重构，对话设置中的提示词将替换内置的总结提示词，请注意修改
                            
                            -- 新增内容 --
                            1. 经过不懈努力，软件已支持Tavo的角色卡导入
                            2. 同步适配了世界书和正则功能
                            3. 支持了提示词中{{user}}和{{char}}的使用
                            4. 支持从 设置 → 显示设置 中配置不同的布局模式和颜色样式，可自行体验
                            
                            -- 其他 --
                            1. 界面效果优化
                            2. 修复了一些已知问题，提升稳定性
                            3. 新增了一些未知的BUG
                        """.trimIndent(),
                        onDismiss = { showUpdateDialog = false }
                    )
                }
            }
        }
    }
}
