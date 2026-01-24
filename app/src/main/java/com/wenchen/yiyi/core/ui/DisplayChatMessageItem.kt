package com.wenchen.yiyi.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil3.compose.AsyncImage
import com.wenchen.yiyi.R
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.coroutineScope
import com.wenchen.yiyi.core.database.entity.ChatMessage
import com.wenchen.yiyi.core.database.entity.MessageContentType
import com.wenchen.yiyi.core.database.entity.MessageType
import com.wenchen.yiyi.core.designSystem.theme.BgGreyDark
import com.wenchen.yiyi.core.designSystem.theme.TextWhite
import com.wenchen.yiyi.core.util.business.ChatUtils
import kotlinx.coroutines.launch
import com.wenchen.yiyi.core.designSystem.component.SpaceHorizontalSmall
import com.wenchen.yiyi.core.designSystem.theme.MaskLight
import com.wenchen.yiyi.feature.aiChat.common.ChatLayoutMode
import com.wenchen.yiyi.feature.aiChat.common.LayoutType

@Composable
fun DisplayChatMessageItem(
    message: ChatMessage,
    userAvatarUrl: String?,
    aiAvatarUrlProvider: (String?) -> String?, // 根据 characterId 获取 AI 头像
    enableSeparator: Boolean,
    chatWindowHazeState: HazeState,
    layoutMode: ChatLayoutMode = ChatLayoutMode(),
    onDeleteMessage: (String) -> Unit,
    onUpdateMessage: (String, String, ((Boolean) -> Unit)?) -> Unit,
    onRegexReplace: (String) -> String,
    onParseMessage: (ChatMessage) -> ChatUtils.ParsedMessage,
    getSegmentHeight: (String, Int) -> Int?,
    onRememberSegmentHeight: (String, Int, Int) -> Unit
) {
    // 1. 解析消息内容（使用 remember 避免重组时重复解析）
    val parsed = remember(message) { onParseMessage(message) }
    val context = LocalContext.current
    val defaultAvatar = "android.resource://${context.packageName}/${R.mipmap.ai_closed}"

    // 2. 确定当前消息显示的头像
    val currentAvatarUrl = when (message.type) {
        MessageType.USER -> userAvatarUrl ?: defaultAvatar
        MessageType.ASSISTANT -> aiAvatarUrlProvider(message.characterId) ?: defaultAvatar
        MessageType.SYSTEM -> defaultAvatar
    }

    when (message.type) {
        MessageType.USER, MessageType.SYSTEM -> {
            ChatMessageItem(
                messageId = message.id,
                content = when (message.contentType) {
                    MessageContentType.TEXT -> parsed.cleanedContent
                    MessageContentType.IMAGE -> message.imgUrl.toString()
                    MessageContentType.VOICE -> ""
                },
                parsedMessage = parsed,
                avatarUrl = currentAvatarUrl,
                messageType = message.type,
                contentType = message.contentType,
                chatWindowHazeState = chatWindowHazeState,
                layoutMode = layoutMode,
                onDeleteMessage = onDeleteMessage,
                onUpdateMessage = onUpdateMessage,
                onRegexReplace = onRegexReplace,
                getSegmentHeight = getSegmentHeight,
                onRememberSegmentHeight = onRememberSegmentHeight
            )
        }

        MessageType.ASSISTANT -> {
            if (message.contentType == MessageContentType.TEXT) {
                // 处理分隔符逻辑
                val parts = if (enableSeparator) {
                    parsed.cleanedContent.split('\\').reversed()
                } else {
                    listOf(parsed.cleanedContent)
                }
                parts.forEachIndexed { index, part ->
                    ChatMessageItem(
                        messageId = message.id,
                        content = part,
                        avatarUrl = currentAvatarUrl,
                        messageType = message.type,
                        contentType = message.contentType,
                        parsedMessage = parsed,
                        chatWindowHazeState = chatWindowHazeState,
                        layoutMode = layoutMode,
                        onDeleteMessage = onDeleteMessage,
                        onUpdateMessage = onUpdateMessage,
                        onRegexReplace = onRegexReplace,
                        getSegmentHeight = getSegmentHeight,
                        onRememberSegmentHeight = onRememberSegmentHeight,
                        segmentIndex = index
                    )
                }
            } else {
                ChatMessageItem(
                    messageId = message.id,
                    content = parsed.cleanedContent,
                    avatarUrl = currentAvatarUrl,
                    messageType = message.type,
                    contentType = message.contentType,
                    chatWindowHazeState = chatWindowHazeState,
                    layoutMode = layoutMode,
                    onDeleteMessage = onDeleteMessage,
                    onUpdateMessage = onUpdateMessage,
                    onRegexReplace = onRegexReplace,
                    getSegmentHeight = getSegmentHeight,
                    onRememberSegmentHeight = onRememberSegmentHeight
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    messageId: String,
    content: String,
    avatarUrl: String?,
    messageType: MessageType,
    contentType: MessageContentType,
    parsedMessage: ChatUtils.ParsedMessage? = null,
    chatWindowHazeState: HazeState,
    layoutMode: ChatLayoutMode,
    onDeleteMessage: (String) -> Unit,
    onUpdateMessage: (String, String, ((Boolean) -> Unit)?) -> Unit,
    onRegexReplace: (String) -> String,
    getSegmentHeight: (String, Int) -> Int?,
    onRememberSegmentHeight: (String, Int, Int) -> Unit,
    segmentIndex: Int = 0
) {
    val visibilityAlpha = remember { mutableFloatStateOf(1f) }
    val isEditing = remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val threshold = with(density) { 168.dp.toPx() }
    val topBarHeight = with(density) { 96.dp.toPx() }

    var isMenuVisible by remember { mutableStateOf(false) }
    var messageItemBounds by remember { mutableStateOf(Rect.Zero) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val itemBottom = bounds.bottom
                // 计算顶部渐隐效果
                visibilityAlpha.floatValue = if (itemBottom < threshold) {
                    ((itemBottom - topBarHeight) / (threshold - topBarHeight)).coerceIn(0f, 1f)
                } else 1f
            }
            .graphicsLayer { alpha = visibilityAlpha.floatValue }
    ) {
        val (avatar, messageCard) = createRefs()

        // 头像显示逻辑
        if (layoutMode.type == LayoutType.STANDARD && messageType != MessageType.SYSTEM) {
            val paddingStart = if (messageType == MessageType.USER) 4.dp else 0.dp
            val paddingEnd = if (messageType == MessageType.USER) 0.dp else 4.dp
            AsyncImage(
                model = avatarUrl,
                contentDescription = "头像",
                modifier = Modifier
                    .size(40.dp)
                    .padding(start = paddingStart, end = paddingEnd)
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
                        }
                    },
                contentScale = ContentScale.Crop
            )
        }

        // 消息气泡卡片
        Card(
            colors = CardDefaults.cardColors(Color.Transparent),
            modifier = Modifier
//                .padding(if (layoutMode == ChatLayoutMode.Full) 0.dp else 4.dp)
                .constrainAs(messageCard) {
                    when (messageType) {
                        MessageType.USER -> {
                            if (layoutMode.type == LayoutType.STANDARD) end.linkTo(avatar.start)
                            else end.linkTo(parent.end)
                            start.linkTo(parent.start, margin = if (layoutMode.type == LayoutType.FULL) 0.dp else 24.dp)
                            horizontalBias = 1f
                        }
                        MessageType.ASSISTANT -> {
                            if (layoutMode.type == LayoutType.STANDARD) start.linkTo(avatar.end)
                            else start.linkTo(parent.start)
                            end.linkTo(parent.end, margin = if (layoutMode.type == LayoutType.FULL) 0.dp else 24.dp)
                            horizontalBias = 0f
                        }
                        MessageType.SYSTEM -> {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                    }
                    width = Dimension.preferredWrapContent
                }
                .onGloballyPositioned { messageItemBounds = it.boundsInWindow() }
        ) {
            Column(
                modifier = Modifier
                    .background(
                        when (messageType) {
                            MessageType.USER -> if (contentType == MessageContentType.TEXT) layoutMode.userBubbleColor else Color.Transparent
                            MessageType.ASSISTANT -> if (contentType == MessageContentType.TEXT) layoutMode.aiBubbleColor else Color.Transparent
                            MessageType.SYSTEM -> MaskLight
                        }
                    )
                    .padding(if (contentType == MessageContentType.TEXT) 8.dp else 0.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                val paddingPx = if (contentType == MessageContentType.TEXT) with(density) { 8.dp.toPx() } else 0f
                                touchPosition = Offset(offset.x + paddingPx, offset.y + paddingPx)
                                isMenuVisible = true
                            }
                        )
                    }
            ) {
                when (contentType) {
                    MessageContentType.TEXT -> {
                        val color = if (messageType == MessageType.USER) layoutMode.userTextColor else layoutMode.aiTextColor
                        val specialTextColor = if (messageType == MessageType.USER) Color.DarkGray else Color.LightGray

                        if (messageType == MessageType.ASSISTANT) {
                            HtmlWebView(
                                modifier = Modifier.fillMaxWidth(),
                                id = "${messageId}_$segmentIndex",
                                html = onRegexReplace(content),
                                textColor = color,
                                height = getSegmentHeight(messageId, segmentIndex),
                                onHeight = { onRememberSegmentHeight(messageId, segmentIndex, it) },
                                onLongClick = { offset ->
                                    val paddingPx = with(density) { 8.dp.toPx() }
                                    touchPosition = Offset(offset.x + paddingPx, offset.y + paddingPx)
                                    isMenuVisible = true
                                }
                            )
                        } else {
                            StyledBracketText(
                                text = content.trim(),
                                normalTextStyle = MaterialTheme.typography.bodyLarge.copy(color = color),
                                specialTextStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = specialTextColor,
                                    fontStyle = FontStyle.Italic
                                )
                            )
                        }
                    }
                    MessageContentType.IMAGE -> {
                        AsyncImage(
                            model = content,
                            contentDescription = "Chat Image",
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .heightIn(max = 250.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    else -> {}
                }
            }

            // 操作菜单 Popup
            if (isMenuVisible) {
                val menuHeightPx = with(density) { 50.dp.toPx() }
                val absoluteTouchY = messageItemBounds.top + touchPosition.y
                val finalY = if (absoluteTouchY < threshold + menuHeightPx) {
                    (touchPosition.y + with(density) { 10.dp.toPx() }).toInt()
                } else {
                    (touchPosition.y - menuHeightPx).toInt()
                }

                Popup(
                    onDismissRequest = { isMenuVisible = false },
                    offset = IntOffset(touchPosition.x.toInt(), finalY)
                ) {
                    MenuContent(
                        onEdit = {
                            isEditing.value = true
                            isMenuVisible = false
                        },
                        onDelete = {
                            onDeleteMessage(messageId)
                            isMenuVisible = false
                        }
                    )
                }
            }
        }

        // 编辑弹窗
        if (isEditing.value) {
            EditMessageDialog(
                originalContent = content,
                onConfirm = { editedContent ->
                    var finalContent = editedContent
                    if (parsedMessage != null) {
                        finalContent = listOfNotNull(
                            parsedMessage.extractedDate,
                            parsedMessage.extractedRole
                        ).joinToString("") + " " + parsedMessage.cleanedContent.replace(content, editedContent)
                    }
                    onUpdateMessage(messageId, finalContent) { isEditing.value = false }
                },
                onDismiss = { isEditing.value = false }
            )
        }
    }
}

@Composable
private fun MenuContent(onEdit: () -> Unit, onDelete: () -> Unit) {
    val iconOffset1 = remember { Animatable(50f) }
    val iconOffset2 = remember { Animatable(50f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { iconOffset1.animateTo(0f, spring(stiffness = Spring.StiffnessHigh)) }
            launch { iconOffset2.animateTo(0f, spring(stiffness = Spring.StiffnessHigh)) }
        }
    }

    Row {
        Icon(
            painterResource(id = R.drawable.edit),
            contentDescription = "Edit",
            tint = TextWhite,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .size(30.dp)
                .offset(y = iconOffset1.value.dp)
                .clickable { onEdit() }
                .background(BgGreyDark)
                .padding(4.dp)
        )
        SpaceHorizontalSmall()
        Icon(
            painterResource(id = R.drawable.delete),
            contentDescription = "Delete",
            tint = TextWhite,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .size(30.dp)
                .offset(y = iconOffset2.value.dp)
                .clickable { onDelete() }
                .background(BgGreyDark)
                .padding(4.dp)
        )
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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(max = 250.dp),
                label = "编辑消息",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = editedText.value,
                onValueChange = { editedText.value = it },
                minLines = 5,
                maxLines = 15,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(editedText.value) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// --- Preview ---

@Preview(showBackground = true)
@Composable
fun PreviewUserMessage() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp).background(Color.Gray)) {
            DisplayChatMessageItem(
                message = ChatMessage(
                    id = "1", content = "你好，这是一条用户消息", type = MessageType.USER,
                    conversationId = "123",
                    characterId = "123",
                    chatUserId = "123",
                    contentType = MessageContentType.TEXT,
                ),
                userAvatarUrl = null,
                aiAvatarUrlProvider = { null },
                enableSeparator = false,
                chatWindowHazeState = remember { HazeState() },
                onDeleteMessage = {},
                onUpdateMessage = { _, _, _ -> },
                onRegexReplace = { it },
                onParseMessage = { ChatUtils.ParsedMessage(cleanedContent = it.content) },
                getSegmentHeight = { _, _ -> null },
                onRememberSegmentHeight = { _, _, _ -> }
            )
        }
    }
}