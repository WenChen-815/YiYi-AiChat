package com.wenchen.yiyi.common

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.wenchen.yiyi.aiChat.common.AIChatManager
import com.wenchen.yiyi.aiChat.ui.ChatDisplayScreen
import com.wenchen.yiyi.aiChat.ui.activity.CharacterEditActivity
import com.wenchen.yiyi.aiChat.ui.activity.ConversationEditActivity
import com.wenchen.yiyi.common.components.IconWithText
import com.wenchen.yiyi.common.theme.AIChatTheme
import com.wenchen.yiyi.common.theme.Gold
import com.wenchen.yiyi.common.theme.GrayText
import com.wenchen.yiyi.common.theme.Pink
import com.wenchen.yiyi.common.theme.WhiteBg
import com.wenchen.yiyi.common.theme.WhiteText
import com.wenchen.yiyi.common.utils.PermissionUtils
import com.wenchen.yiyi.common.utils.StatusBarUtil
import com.wenchen.yiyi.config.common.ConfigManager
import com.wenchen.yiyi.config.ui.activity.ConfigActivity
import com.wenchen.yiyi.home.ui.HomeScreen
import com.wenchen.yiyi.worldBook.ui.activity.WorldBookEditActivity
import com.wenchen.yiyi.worldBook.ui.activity.WorldBookListActivity
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

        // 请求权限
        PermissionUtils.checkAndRequestStoragePermission(this, requestPermissionLauncher)
        // 请求悬浮窗权限
        PermissionUtils.checkAndRequestOverlayPermission(this, overlayPermissionLauncher)

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
        var showAddPopup by remember { mutableStateOf(false) }
        val hasOverlayPermission = remember { mutableStateOf(PermissionUtils.hasOverlayPermission(this)) }
        Scaffold(
            topBar = {
                when (currentPosition) {
                    0 ->
                        TopAppBar(
                            title = {
                                Text(
                                    text = "角色列表",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        MaterialTheme.colorScheme.onBackground,
                                    ),
                                )
                            },
                            actions = {
//                                TextButton(onClick = {
//                                    val intent = Intent(
//                                        this@MainActivity,
//                                        ConfigActivity::class.java
//                                    )
//                                    startActivity(intent)
//                                }) {
//                                    Text("设置")
//                                }
                                IconWithText(
                                    icon = Icons.Rounded.Book,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    text = "世界",
                                    modifier = Modifier.clickable{
                                        startActivity(Intent(
                                            this@MainActivity,
                                            WorldBookListActivity::class.java
                                        ))}
                                        .padding(horizontal = 8.dp),
                                    iconModifier = Modifier.size(24.dp),
                                    textModifier = Modifier.padding(top = 4.dp),
                                    textStyle = MaterialTheme.typography.labelSmall.copy(MaterialTheme.colorScheme.primary,)
                                )
                                IconWithText(
                                    icon = Icons.Rounded.Settings,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    text = "设置",
                                    modifier = Modifier.clickable{
                                        startActivity(Intent(
                                            this@MainActivity,
                                            ConfigActivity::class.java
                                        ))}
                                        .padding(horizontal = 8.dp),
                                    iconModifier = Modifier.size(24.dp),
                                    textModifier = Modifier.padding(top = 4.dp),
                                    textStyle = MaterialTheme.typography.labelSmall.copy(MaterialTheme.colorScheme.primary,)
                                )
                            },
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                ),
                        )

                    1 ->
                        TopAppBar(
                            title = {
                                Text(
                                    text = "聊天列表",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        MaterialTheme.colorScheme.onBackground,
                                    ),
                                )
                            },
                            actions = {
                                TextButton(onClick = {
                                    val intent = Intent(
                                        this@MainActivity,
                                        ConversationEditActivity::class.java
                                    )
                                    startActivity(intent)
                                }) {
                                    Text("创建群组")
                                }
                            },
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                ),
                        )
                }
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier =
                        Modifier
//                        .background(MaterialTheme.colorScheme.primary) // 这样设置是无效的，应该用containerColor
                            .fillMaxWidth()
                            .height(66.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().weight(1f).clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ){ currentPosition = 0 },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "角色",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = when(currentPosition) {
                                        0 -> FontWeight.Bold
                                        else -> FontWeight.Normal
                                    }
                                ),
                                color = if (currentPosition == 0) MaterialTheme.colorScheme.onBackground else GrayText,
                            )
                        }

                        // 中间新增按钮
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新增",
                            tint = WhiteText,
                            modifier =
                                Modifier
                                    .size(48.dp, 36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        brush =
                                            Brush.horizontalGradient(
                                                colors = listOf(Pink, Gold),
                                            ),
                                    )
                                    .clickable { showAddPopup = true },
                        )
                        // 弹出菜单
                        if (showAddPopup) {
                            Popup(
                                alignment = Alignment.TopCenter,
                                onDismissRequest = { showAddPopup = false }
                            ) {
                                Card(
                                    modifier = Modifier
                                        .padding(bottom = 48.dp)
                                        .width(150.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors().copy(
                                        containerColor = WhiteBg
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        // 新增角色选项
                                        Text(
                                            text = "新增角色",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showAddPopup = false
                                                    startActivity(
                                                        Intent(
                                                            this@MainActivity,
                                                            CharacterEditActivity::class.java,
                                                        ),
                                                    )
                                                }
                                                .padding(12.dp),
                                            style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.primary)
                                        )

                                        HorizontalDivider()

                                        // 新增世界选项
                                        Text(
                                            text = "新增世界",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showAddPopup = false
                                                    startActivity(
//                                                        Intent(
//                                                            this@MainActivity,
//                                                            WorldBookListActivity::class.java
//                                                        )
                                                        Intent(this@MainActivity, WorldBookEditActivity::class.java)
                                                    )
                                                }
                                                .padding(12.dp),
                                            style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxHeight().weight(1f).clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ){ currentPosition = 1 },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "聊天",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = when(currentPosition) {
                                        1 -> FontWeight.Bold
                                        else -> FontWeight.Normal
                                    }
                                ),
                                color = if (currentPosition == 1) MaterialTheme.colorScheme.onBackground else GrayText,
                            )
                        }
                    }
                }
            },
//            floatingActionButton = {
//                FloatingActionButton(
//                    onClick = {
//                        startActivity(Intent(this@MainActivity, TestActivity::class.java))
//                    },
//                    containerColor = Pink,
//                    modifier = Modifier.padding(16.dp)
//                ) {
//                    Text(
//                        text = "测试区域",
//                        style = MaterialTheme.typography.titleMedium.copy(
//                            fontWeight = FontWeight.Bold
//                        ),
//                        color = Color.White,
//                    )
//                }
//            }
        ) { padding ->
            Box(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(padding),
            ) {
                when (currentPosition) {
                    0 -> HomeScreen(this@MainActivity)
                    1 -> ChatDisplayScreen(this@MainActivity)
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                Log.d("MainActivity", "权限已授予")
            } else {
                // 权限被拒绝，显示提示信息
                Toast.makeText(this, "需要权限才能正常使用", Toast.LENGTH_SHORT).show()
            }
        }

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                Log.d("MainActivity", "悬浮窗权限已授予")
            } else {
                Toast.makeText(this, "悬浮窗权限未授予，部分功能可能受限", Toast.LENGTH_SHORT).show()
            }
        }
}
