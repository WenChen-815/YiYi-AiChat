package com.wenchen.yiyi.feature.aiChat.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.constraintlayout.compose.ConstraintLayout
import coil3.compose.AsyncImage
import com.wenchen.yiyi.R
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.coroutineScope
import com.wenchen.yiyi.feature.aiChat.entity.ChatMessage
import com.wenchen.yiyi.feature.aiChat.entity.ConversationType
import com.wenchen.yiyi.feature.aiChat.entity.MessageContentType
import com.wenchen.yiyi.feature.aiChat.entity.MessageType
import com.wenchen.yiyi.core.common.components.SettingTextFieldItem
import com.wenchen.yiyi.core.common.components.StyledBracketText
import com.wenchen.yiyi.core.common.theme.BlackBg
import com.wenchen.yiyi.core.common.theme.BlackText
import com.wenchen.yiyi.core.common.theme.Gold
import com.wenchen.yiyi.core.common.theme.HalfTransparentBlack
import com.wenchen.yiyi.core.common.theme.IconBg
import com.wenchen.yiyi.core.common.theme.WhiteBg
import com.wenchen.yiyi.core.common.theme.WhiteText
import com.wenchen.yiyi.core.common.utils.ChatUtil
import com.wenchen.yiyi.core.util.toast.ToastUtils
import com.wenchen.yiyi.feature.aiChat.vm.BaseChatViewModel
import kotlinx.coroutines.launch

@Composable
fun DisplayChatMessageItem(
    message: ChatMessage,
    viewModel: BaseChatViewModel,
    chatWindowHazeState: HazeState,
) {
    // 解析AI回复中的日期和角色名称
    val parseMessage = ChatUtil.parseMessage(message)
    val uiState by viewModel.uiState.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val userConfig by viewModel.userConfigState.userConfig.collectAsState()

    when (message.type) {
        MessageType.USER, MessageType.SYSTEM -> ChatMessageItem(
            viewModel = viewModel,
            messageId = message.id,
            content = when (message.contentType) {
                MessageContentType.TEXT -> parseMessage.cleanedContent
                MessageContentType.IMAGE -> message.imgUrl.toString()
                MessageContentType.VOICE -> ""
            },
            parsedMessage = parseMessage,
            avatarUrl = userConfig?.userAvatarPath
                ?: "android.resource://${LocalContext.current.packageName}/${R.mipmap.ai_closed}",
            messageType = message.type,
            contentType = message.contentType,
            chatWindowHazeState = chatWindowHazeState
        )

        MessageType.ASSISTANT -> {
            val avatarUrl = if (conversation.type == ConversationType.SINGLE) {
                uiState.currentCharacter?.avatarPath
                    ?: "android.resource://${LocalContext.current.packageName}/${R.mipmap.ai_closed}"
            } else {
                uiState.currentCharacters.find { it.aiCharacterId == message.characterId }?.avatarPath
                    ?: "android.resource://${LocalContext.current.packageName}/${R.mipmap.ai_closed}"
            }
            if (message.contentType == MessageContentType.TEXT) {
                val parts = if (userConfig?.enableSeparator == true) {
                    parseMessage.cleanedContent.split('\\').filter { it.isNotBlank() }.reversed()
                } else {
                    listOf(parseMessage.cleanedContent)
                }
                for (i in parts.indices) {
                    ChatMessageItem(
                        viewModel = viewModel,
                        messageId = message.id,
                        content = parts[i],
                        avatarUrl = avatarUrl,
                        messageType = message.type,
                        contentType = message.contentType,
                        parsedMessage = parseMessage,
                        chatWindowHazeState = chatWindowHazeState
                    )
                }
            } else {
                ChatMessageItem(
                    viewModel = viewModel,
                    messageId = message.id,
                    content = parseMessage.cleanedContent,
                    avatarUrl = if (uiState.currentCharacter?.avatarPath?.isNotBlank() == true) {
                        uiState.currentCharacter?.avatarPath
                    } else "android.resource://${LocalContext.current.packageName}/${R.mipmap.ai_closed}",
                    messageType = message.type,
                    contentType = message.contentType,
                    chatWindowHazeState = chatWindowHazeState
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    viewModel: BaseChatViewModel,
    messageId: String,
    content: String,
    avatarUrl: String?,
    messageType: MessageType,
    contentType: MessageContentType,
    parsedMessage: ChatUtil.ParsedMessage? = null,
    chatWindowHazeState: HazeState
) {
    val visibilityAlpha = remember { mutableFloatStateOf(1f) }
    val isEditing = remember { mutableStateOf(false) }
    val threshold = with(LocalDensity.current) { 168.dp.toPx() } // 设定阈值
    val topBarHeight = with(LocalDensity.current) { 96.dp.toPx() } // 设定顶部栏高度
    var isMenuVisible by remember { mutableStateOf(false) }
    var messageItemBounds by remember { mutableStateOf(Rect.Zero) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val itemBottom = bounds.bottom

                // 计算透明度
                visibilityAlpha.floatValue = if (itemBottom < threshold) {
                    ((itemBottom - topBarHeight) / (threshold - topBarHeight)).coerceIn(0f, 1f)
                } else 1f
            }
            .graphicsLayer {
                alpha = visibilityAlpha.floatValue
            }
    ) {
        val (avatar, messageCard) = createRefs()
        if (messageType != MessageType.SYSTEM) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "头像",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .constrainAs(avatar) {
                        when (messageType) {
                            MessageType.USER -> {
                                top.linkTo(parent.top)
                                end.linkTo(parent.end)
                            }

                            MessageType.ASSISTANT -> {
                                top.linkTo(parent.top)
                                start.linkTo(parent.start)
                            }

                            MessageType.SYSTEM -> {}
                        }
                    },
                contentScale = ContentScale.Crop
            )
        }

        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(4.dp)
                .constrainAs(messageCard) {
                    when (messageType) {
                        MessageType.USER -> {
                            top.linkTo(parent.top)
                            end.linkTo(avatar.start)
                        }

                        MessageType.ASSISTANT -> {
                            top.linkTo(parent.top)
                            start.linkTo(avatar.end)
                        }

                        MessageType.SYSTEM -> {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                    }
                }
                .onGloballyPositioned { coordinates ->
                    messageItemBounds = coordinates.boundsInWindow()
                },
            colors = CardDefaults.cardColors(Color.Transparent)

        ) {
            Column(
                modifier = Modifier
                    .background(
                        when (messageType) {
                            MessageType.USER -> if (contentType == MessageContentType.TEXT) Gold else Color.Transparent
                            MessageType.ASSISTANT -> if (contentType == MessageContentType.TEXT) BlackBg else Color.Transparent
                            MessageType.SYSTEM -> HalfTransparentBlack.copy(0.55f)
                        }
                    )
                    .padding(if (contentType == MessageContentType.TEXT) 8.dp else 0.dp)
                    // combinedClickable 获取不到点击位置，使用 pointerInput 替代
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { /* 处理点击事件 */ },
                            onLongPress = { offset ->
                                touchPosition = offset
                                isMenuVisible = true
                            }
                        )
                    }
            ) {
                when (contentType) {
                    MessageContentType.TEXT -> {
                        val color = when (messageType) {
                            MessageType.USER -> BlackText
                            MessageType.ASSISTANT, MessageType.SYSTEM -> WhiteText
                        }
                        val specialTextColor = when (messageType) {
                            MessageType.USER -> Color.DarkGray
                            MessageType.ASSISTANT -> Color.LightGray
                            MessageType.SYSTEM -> WhiteText
                        }
                        StyledBracketText(
                            text = content.trim(),
                            normalTextStyle = MaterialTheme.typography.bodyLarge.copy(color = color),
                            specialTextStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = specialTextColor,
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }

                    MessageContentType.IMAGE -> {
                        AsyncImage(
                            model = content,
                            contentDescription = "聊天图片",
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .heightIn(max = 250.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    MessageContentType.VOICE -> {}
                }
            }
            if (isMenuVisible) {
                // 计算消息项中心点的 Y 坐标
                val itemCenterY = (messageItemBounds.top + messageItemBounds.bottom) / 2
                // 计算控件高度
                val itemHeight = messageItemBounds.height
                // 获取屏幕高度
                val screenHeight = with(LocalDensity.current) {
                    LocalWindowInfo.current.containerSize.height.toDp().value * density
                }
                // 判断显示位置：
                // 如果消息项中心点在屏幕上方 3/4 区域内，则显示在控件下方
                // 否则显示在控件上方
                val showBelow = itemCenterY < screenHeight * 0.75

                val yOffset = if (showBelow) itemHeight.toInt() else 0

            Popup(
                onDismissRequest = { isMenuVisible = false }, // 点击外部关闭菜单
                offset = IntOffset(
                    x = touchPosition.x.toInt(),
                    /**
                     * 知识点
                     * popup的坐标原点在于父控件的左上角,是相对坐标，而不是整个屏幕左上角的绝对坐标
                     * 但 messageItemBounds.value = coordinates.boundsInWindow() 获取到的位置是基于整个屏幕的坐标
                     * 直接应用会导致偏移甚远
                     */
                    y = if (itemHeight > screenHeight * 0.75) {
                        touchPosition.y.toInt()
                    } else {
//                        with(LocalDensity.current) { (-15).dp.toPx().toInt() }
                        yOffset
                    }
                )
            ) {
                val iconOffset1 = remember { Animatable(50f) } // 第一个图标初始位置偏移
                val iconOffset2 = remember { Animatable(50f) } // 第二个图标初始位置偏移
                // 触发动画
                LaunchedEffect(isMenuVisible) {
                    if (isMenuVisible) {
                        // 并行执行动画
                        coroutineScope {
                            launch {
                                iconOffset1.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessHigh
                                    )
                                )
                            }
                            launch {
                                iconOffset2.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessHigh
                                    )
                                )
                            }
                        }
                    } else {
                        // 重置位置
                        iconOffset1.snapTo(50f)
                        iconOffset2.snapTo(50f)
                    }
                }
                Row {
                    // 编辑按钮
                    Icon(
                        painterResource(id = R.drawable.edit),
                        contentDescription = "编辑",
                        tint = WhiteText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .size(30.dp)
                            .offset(y = iconOffset1.value.dp) // 应用动画偏移
                            .clickable {
                                isEditing.value = true
                                isMenuVisible = false
                            }
                            .background(IconBg)
//                            .hazeEffect(chatWindowHazeState)
                                .padding(4.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        // 删除按钮
                        Icon(
                            painterResource(id = R.drawable.delete),
                            contentDescription = "删除",
                            tint = WhiteText,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .size(30.dp)
                                .offset(y = iconOffset2.value.dp) // 应用动画偏移
                                .clickable {
                                    viewModel.deleteOneMessage(messageId)
                                    isMenuVisible = false
                                }
                                .background(IconBg)
//                            .hazeEffect(chatWindowHazeState)
                                .padding(4.dp)

                        )
                    }
                }
            }
        }
        if (isEditing.value) {
            EditMessageDialog(
                originalContent = content,
                onConfirm = { editedContent ->
                    var finalContent = editedContent
                    if (parsedMessage != null) {
                        finalContent = listOfNotNull(
                            parsedMessage.extractedDate,
                            parsedMessage.extractedRole
                        ).joinToString("") + " " + parsedMessage.cleanedContent.replace(
                            content,
                            editedContent
                        )
                    }
                    viewModel.updateMessageContent(messageId, finalContent) {
                        isEditing.value = false
                    }
                },
                onDismiss = {
                    isEditing.value = false
                }
            )
        }
    }
}

@Composable
fun EditMessageDialog(
    originalContent: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val editedText = remember { mutableStateOf(originalContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            SettingTextFieldItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 250.dp),
                label = "编辑消息",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = editedText.value,
                onValueChange = { editedText.value = it },
                placeholder = { Text("请输入编辑后的消息") },
                minLines = 5,
                maxLines = 15,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(editedText.value)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
