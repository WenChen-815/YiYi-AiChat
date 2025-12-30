package com.wenchen.yiyi.feature.aiChat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wenchen.yiyi.feature.aiChat.entity.Conversation
import com.wenchen.yiyi.feature.aiChat.entity.ConversationType
import com.wenchen.yiyi.feature.aiChat.vm.ChatViewModel
import com.wenchen.yiyi.core.common.entity.AICharacter
import com.wenchen.yiyi.core.common.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

@Composable
fun NavigationDrawerContent(
    viewModel: ChatViewModel,
    onNavConfigClick: () -> Unit,
    onNavSwitchModelClick: () -> Unit,
    onNavClearChatClick: () -> Unit,
    onNavAboutClick: () -> Unit,
    onEditClick: (AICharacter) -> Unit = {},
    onGotoConversationEdit: () -> Unit,
    onDeleteClick: (AICharacter) -> Unit = {},
    onDeleteGroupClick: (Conversation) -> Unit = {},
    themeBgColor: Color = HalfTransparentBlack,
    hazeState: HazeState,
) {
    val density = LocalDensity.current.density
    val windowInfo = LocalWindowInfo.current
    val screenWidthDp = windowInfo.containerSize.width / density
    val uiState = viewModel.uiState.collectAsState().value
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        ModalDrawerSheet(
            // 覆盖默认的窗口内边距，允许内容延伸到状态栏
            windowInsets = WindowInsets(0, 0, 0, 0),
            drawerContentColor = if (isSystemInDarkTheme()) WhiteText else BlackText,
            drawerContainerColor = Color.Transparent,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .fillMaxHeight()
                .width(screenWidthDp.dp * 0.8f)
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        tint = null,
                        backgroundColor = themeBgColor.copy(0.95f),
                        blurRadius = 111.dp,
                    )
                )
        ) {
            Text(
                text = "对话设定",
                style = MaterialTheme.typography.titleMedium.copy(WhiteText),
                modifier = Modifier.padding(12.dp, 56.dp, 12.dp, 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WhiteBg.copy(0.1f)),
                verticalArrangement = Arrangement.Bottom
            ) {
                YiYiChatDrawerItem(
                    label = "我的昵称",
                    description = uiState.conversation.playerName,
                    onClick = onGotoConversationEdit,
                )
                YiYiChatDrawerItem(
                    label = "我的性别",
                    description = uiState.conversation.playGender,
                    onClick = onGotoConversationEdit,
                )
                YiYiChatDrawerItem(
                    label = "我的描述",
                    description = if (uiState.conversation.playerDescription.length > 10) "${
                        uiState.conversation.playerDescription.substring(
                            0,
                            10
                        )
                    }..." else uiState.conversation.playerDescription,
                    onClick = onGotoConversationEdit,
                )
                YiYiChatDrawerItem(
                    label = "当前场景",
                    description = if (uiState.conversation.chatSceneDescription.length > 10) "${
                        uiState.conversation.chatSceneDescription.substring(
                            0,
                            10
                        )
                    }..." else uiState.conversation.chatSceneDescription,
                    onClick = onGotoConversationEdit,
                )
            }

            Text(
                text = "全局设置",
                style = MaterialTheme.typography.titleMedium.copy(WhiteText),
                modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WhiteBg.copy(0.1f)),
                verticalArrangement = Arrangement.Bottom
            ) {
                YiYiChatDrawerItem(
                    label = "API配置",
                    onClick = onNavConfigClick,
                )
                YiYiChatDrawerItem(
                    label = "切换模型",
                    description = uiState.currentModelName,
                    onClick = onNavSwitchModelClick,
                )
            }

            Text(
                text = "对话设置",
                style = MaterialTheme.typography.titleMedium.copy(WhiteText),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WhiteBg.copy(0.1f)),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (uiState.conversation.type == ConversationType.SINGLE) {
                    YiYiChatDrawerItem(
                        label = "编辑角色",
                        onClick = { onEditClick(uiState.currentCharacter!!) },
                    )
                    YiYiChatDrawerItem(
                        label = "删除角色",
                        onClick = { onDeleteClick(uiState.currentCharacter!!) },
                    )
                }
                if (uiState.conversation.type == ConversationType.GROUP) {
                    YiYiChatDrawerItem(
                        label = "删除会话",
                        onClick = { onDeleteGroupClick(uiState.conversation) },
                    )
                }
                YiYiChatDrawerItem(
                    label = "清空聊天记录",
                    onClick = onNavClearChatClick,
                )
            }

            Text(
                text = "更多",
                style = MaterialTheme.typography.titleMedium.copy(WhiteText),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WhiteBg.copy(0.1f)),
                verticalArrangement = Arrangement.Bottom
            ) {
                YiYiChatDrawerItem(
                    label = "关于",
                    onClick = onNavAboutClick,
                )
            }
        }
    }
}