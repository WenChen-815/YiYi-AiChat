package com.wenchen.yiyi.core.designSystem.component

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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.constraintlayout.compose.ConstraintLayout
import coil3.compose.AsyncImage
import com.wenchen.yiyi.R
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.coroutineScope
import com.wenchen.yiyi.core.database.entity.ChatMessage
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.core.database.entity.MessageContentType
import com.wenchen.yiyi.core.database.entity.MessageType
import com.wenchen.yiyi.core.common.theme.BlackBg
import com.wenchen.yiyi.core.common.theme.BlackText
import com.wenchen.yiyi.core.common.theme.Gold
import com.wenchen.yiyi.core.common.theme.HalfTransparentBlack
import com.wenchen.yiyi.core.common.theme.IconBg
import com.wenchen.yiyi.core.common.theme.WhiteText
import com.wenchen.yiyi.core.util.business.ChatUtils
import com.wenchen.yiyi.feature.aiChat.viewmodel.BaseChatViewModel
import kotlinx.coroutines.launch

@Composable
fun DisplayChatMessageItem(
    message: ChatMessage,
    viewModel: BaseChatViewModel,
    chatWindowHazeState: HazeState,
) {
    // 解析AI回复中的日期和角色名称
    val parseMessage = ChatUtils.parseMessage(message)
    val chatUiState by viewModel.chatUiState.collectAsState()
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
                chatUiState.currentCharacter?.avatar
                    ?: "android.resource://${LocalContext.current.packageName}/${R.mipmap.ai_closed}"
            } else {
                chatUiState.currentCharacters.find { it.id == message.characterId }?.avatar
                    ?: "android.resource://${LocalContext.current.packageName}/${R.mipmap.ai_closed}"
            }
            if (message.contentType == MessageContentType.TEXT) {
                val parts = if (userConfig?.enableSeparator == true) {
                    parseMessage.cleanedContent.split('\\').reversed()
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
                    avatarUrl = avatarUrl,
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
    parsedMessage: ChatUtils.ParsedMessage? = null,
    chatWindowHazeState: HazeState
) {
    val visibilityAlpha = remember { mutableFloatStateOf(1f) }
    val isEditing = remember { mutableStateOf(false) }
    val threshold = with(LocalDensity.current) { 168.dp.toPx() } // 设定阈值
    val topBarHeight = with(LocalDensity.current) { 96.dp.toPx() } // 设定顶部栏高度
    var isMenuVisible by remember { mutableStateOf(false) }
    var messageItemBounds by remember { mutableStateOf(Rect.Zero) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

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
                                // 加上 8.dp 的 padding 偏移，使坐标相对于 Card
                                val paddingPx = if (contentType == MessageContentType.TEXT) with(density) { 8.dp.toPx() } else 0f
                                touchPosition = Offset(offset.x + paddingPx, offset.y + paddingPx)
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
                        // 拆分内容
                        val segments = remember(content) { splitMessageContent(content) }

                        Column {
                            segments.forEachIndexed { index, segment ->
                                when (segment) {
                                    is MessageSegment.Text -> {
                                        StyledBracketText(
                                            text = segment.content.trim(),
                                            normalTextStyle = MaterialTheme.typography.bodyLarge.copy(color = color),
                                            specialTextStyle = MaterialTheme.typography.bodyLarge.copy(
                                                color = specialTextColor,
                                                fontStyle = FontStyle.Italic
                                            )
                                        )
                                    }
                                    is MessageSegment.Html -> {
                                        HtmlWebView(
                                            modifier = Modifier.fillMaxWidth(),
                                            id = "${messageId}_$index", // 使用唯一片段ID
                                            html = segment.content,
                                            textColor = color,
                                            height = viewModel.findSegmentHeight(messageId, index),
                                            onHeight = { height ->
                                                viewModel.rememberSegmentHeight(messageId, index, height)
                                            },
                                            onLongClick = { offset ->
                                                val paddingPx = with(density) { 8.dp.toPx() }
                                                touchPosition = Offset(offset.x + paddingPx, offset.y + paddingPx)
                                                isMenuVisible = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // 识别 HTML 的简单启发式算法
//                        val isHtmlContent = remember(content) {
//                            val trimmed = content.trim()
//                            (trimmed.contains("<") && trimmed.contains(">")) ||
//                                    trimmed.contains("class=") ||
//                                    trimmed.contains("style=")
//                        }
//                        if (isHtmlContent) {
//                            HtmlWebView(
//                                id = messageId,
//                                html = content,
//                                textColor = color,
//                                height = viewModel.findMessageItemHeight(messageId),
//                                modifier = Modifier.fillMaxWidth(),
//                                onHeight = { height ->
//                                    // 处理高度回调
//                                    viewModel.rememberMessageItemHeight(messageId, height)
//                                },
//                                onLongClick = { offset ->
//                                    // 因为 WebView 放在带有 padding(8.dp) 的 Column 中
//                                    // touchPosition 需要加上这个偏移量，以匹配 Column 的坐标系
//                                    val paddingPx = with(density) { 8.dp.toPx() }
//                                    touchPosition = Offset(
//                                        x = offset.x + paddingPx,
//                                        y = offset.y + paddingPx
//                                    )
//                                    isMenuVisible = true
//                                }
//                            )
//                        }
//                        else {
//                            val specialTextColor = when (messageType) {
//                                MessageType.USER -> Color.DarkGray
//                                MessageType.ASSISTANT -> Color.LightGray
//                                MessageType.SYSTEM -> WhiteText
//                            }
//                            StyledBracketText(
//                                text = content.trim(),
//                                normalTextStyle = MaterialTheme.typography.bodyLarge.copy(color = color),
//                                specialTextStyle = MaterialTheme.typography.bodyLarge.copy(
//                                    color = specialTextColor,
//                                    fontStyle = FontStyle.Italic
//                                )
//                            )
//                        }


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
                // 计算控件高度
//                val itemHeight = messageItemBounds.height

                // 使用 LocalConfiguration 获取高度
//                val configuration = LocalConfiguration.current
//                val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

                // --- 跟随手指位置 ---
                // 预估菜单高度
                val menuHeightPx = with(density) { 50.dp.toPx() }
                // 计算手指在屏幕上的绝对 Y 坐标
                val absoluteTouchY = messageItemBounds.top + touchPosition.y

                // 判断逻辑：如果手指位置太靠顶部，则在手指下方弹出；否则在手指上方弹出
                val finalY = if (absoluteTouchY < threshold + menuHeightPx) {
                    (touchPosition.y + with(density) { 10.dp.toPx() }).toInt()
                } else {
                    (touchPosition.y - menuHeightPx).toInt()
                }

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
//                    y = if (itemHeight > screenHeight * 0.75) {
//                        touchPosition.y.toInt()
//                    } else {
//                        with(LocalDensity.current) { (-15).dp.toPx().toInt() }
//                        yOffset
//                    }
                    y = finalY
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
/**
 * 将消息内容拆分为文本和HTML片段，支持处理嵌套标签
 */
private fun splitMessageContent(content: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    // 识别 HTML 标签的正则，忽略大小写
    // 使用 [\s\S]*? 来匹配包括换行在内的所有字符以捕获多行注释
    val tagRegex = Regex("""<!--[\s\S]*?-->|<(/?)([a-zA-Z1-6]+)[^>]*>""", RegexOption.IGNORE_CASE)
    val matches = tagRegex.findAll(content).toList()

    if (matches.isEmpty()) {
        return listOf(MessageSegment.Text(content))
    }

    var lastIndex = 0
    var i = 0
    while (i < matches.size) {
        val match = matches[i]

        // 添加标签前的文本
        if (match.range.first > lastIndex) {
            val text = content.substring(lastIndex, match.range.first)
            if (text.isNotBlank()) segments.add(MessageSegment.Text(text))
        }

        // 处理 HTML 注释：注释直接作为 HTML 片段
        if (match.value.startsWith("<!--")) {
            segments.add(MessageSegment.Html(match.value))
            lastIndex = match.range.last + 1
            i++
            continue
        }

        // 获取标签信息，索引 1 为 (/?), 索引 2 为 ([a-zA-Z1-6]+)
        val isClosing = match.groupValues[1] == "/"
        val tagName = match.groupValues[2]
        val isSelfClosing = match.value.endsWith("/>")

        if (tagName.isEmpty()) {
            // 如果由于某种原因 tagName 为空且不是注释，则作为普通 HTML 处理
            segments.add(MessageSegment.Html(match.value))
            lastIndex = match.range.last + 1
            i++
            continue
        }

        if (isClosing || isSelfClosing) {
            // 孤立的闭合标签或自闭合标签，直接作为 HTML 片段
            segments.add(MessageSegment.Html(match.value))
            lastIndex = match.range.last + 1
            i++
        } else {
            // 起始标签，寻找平衡的闭合标签（考虑嵌套）
            var depth = 1
            var j = i + 1
            var blockEnd = match.range.last + 1
            var foundEnd = false

            while (j < matches.size) {
                val nextMatch = matches[j]

                // 跳过注释，注释不参与标签嵌套计算
                if (nextMatch.value.startsWith("<!--")) {
                    j++
                    continue
                }

                val nextIsClosing = nextMatch.groupValues[1] == "/"
                val nextTagName = nextMatch.groupValues[2]

                if (nextTagName.equals(tagName, ignoreCase = true)) {
                    if (nextIsClosing) {
                        depth--
                    } else if (!nextMatch.value.endsWith("/>")) {
                        depth++
                    }
                }

                if (depth == 0) {
                    blockEnd = nextMatch.range.last + 1
                    foundEnd = true
                    i = j + 1
                    break
                }
                j++
            }

            if (foundEnd) {
                // 找到了匹配的闭合标签，将整个范围作为 HTML 片段
                segments.add(MessageSegment.Html(content.substring(match.range.first, blockEnd)))
                lastIndex = blockEnd
            } else {
                // 未找到匹配，仅将当前标签视为 HTML 并继续
                segments.add(MessageSegment.Html(match.value))
                lastIndex = match.range.last + 1
                i++
            }
        }
    }

    if (lastIndex < content.length) {
        val remaining = content.substring(lastIndex)
        if (remaining.isNotBlank()) segments.add(MessageSegment.Text(remaining))
    }

    // 合并相邻的 HTML 片段（包括它们之间的空白字符），减少 WebView 数量并防止布局断裂
    val mergedSegments = mutableListOf<MessageSegment>()
    segments.forEach { segment ->
        when (val last = mergedSegments.lastOrNull()) {
            is MessageSegment.Html if segment is MessageSegment.Html -> {
                mergedSegments[mergedSegments.size - 1] =
                    MessageSegment.Html(last.content + segment.content)
            }

            is MessageSegment.Html if segment is MessageSegment.Text && segment.content.isBlank() -> {
                mergedSegments.add(segment)
            }

            is MessageSegment.Text if last.content.isBlank() && segment is MessageSegment.Html &&
                    mergedSegments.size >= 2 && mergedSegments[mergedSegments.size - 2] is MessageSegment.Html -> {
                val prevHtml = mergedSegments[mergedSegments.size - 2] as MessageSegment.Html
                mergedSegments.removeAt(mergedSegments.size - 1)
                mergedSegments[mergedSegments.size - 1] =
                    MessageSegment.Html(prevHtml.content + last.content + segment.content)
            }

            else -> {
                mergedSegments.add(segment)
            }
        }
    }

    return if (mergedSegments.isEmpty() && content.isNotEmpty()) listOf(MessageSegment.Text(content)) else mergedSegments
}
// 定义消息片段类型
private sealed class MessageSegment {
    data class Text(val content: String) : MessageSegment()
    data class Html(val content: String) : MessageSegment()
}