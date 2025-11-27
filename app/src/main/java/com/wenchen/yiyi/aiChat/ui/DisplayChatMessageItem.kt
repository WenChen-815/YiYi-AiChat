package com.wenchen.yiyi.aiChat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil3.compose.AsyncImage
import com.wenchen.yiyi.R
import com.wenchen.yiyi.aiChat.entity.ChatMessage
import com.wenchen.yiyi.aiChat.entity.ConversationType
import com.wenchen.yiyi.aiChat.entity.MessageContentType
import com.wenchen.yiyi.aiChat.entity.MessageType
import com.wenchen.yiyi.aiChat.vm.ChatViewModel
import com.wenchen.yiyi.common.theme.BlackBg
import com.wenchen.yiyi.common.theme.BlackText
import com.wenchen.yiyi.common.theme.Gold
import com.wenchen.yiyi.common.theme.HalfTransparentBlack
import com.wenchen.yiyi.common.theme.WhiteText
import com.wenchen.yiyi.common.utils.ChatUtil
import com.wenchen.yiyi.config.common.ConfigManager

@Composable
fun DisplayChatMessageItem(
    message: ChatMessage,
    viewModel: ChatViewModel,
) {
    // 解析AI回复中的日期和角色名称
    var cleanedContent = ChatUtil.parseMessage(message)
    val uiState = viewModel.uiState.collectAsState().value
    when (message.type) {
        MessageType.USER, MessageType.SYSTEM -> ChatMessageItem(
            content = when (message.contentType) {
                MessageContentType.TEXT -> cleanedContent
                MessageContentType.IMAGE -> message.imgUrl.toString()
                MessageContentType.VOICE -> ""
            },
            avatarUrl = ConfigManager().getUserAvatarPath()
                ?: "android.resource://${LocalContext.current.packageName}/${R.mipmap.ai_closed}",
            messageType = message.type,
            contentType = message.contentType
        )

        MessageType.ASSISTANT -> {
            val avatarUrl = if (uiState.conversation.type == ConversationType.SINGLE) {
                uiState.currentCharacter?.avatarPath
                    ?: "android.resource://${LocalContext.current.packageName}/${R.mipmap.ai_closed}"
            } else {
                uiState.currentCharacters.find { it.aiCharacterId == message.characterId }?.avatarPath
                    ?: "android.resource://${LocalContext.current.packageName}/${R.mipmap.ai_closed}"
            }
            if (message.contentType == MessageContentType.TEXT) {
                // 分割AI消息内容
                val parts = cleanedContent.split('\\').filter { it.isNotBlank() }.reversed()
                for (i in parts.indices) {
                    ChatMessageItem(
                        content = parts[i],
                        avatarUrl = avatarUrl,
                        messageType = message.type,
                        contentType = message.contentType
                    )
                }
            } else {
                ChatMessageItem(
                    content = cleanedContent,
                    avatarUrl = if (uiState.currentCharacter?.avatarPath?.isNotBlank() == true) {
                        uiState.currentCharacter.avatarPath
                    } else "android.resource://${LocalContext.current.packageName}/${R.mipmap.ai_closed}",
                    messageType = message.type,
                    contentType = message.contentType
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    content: String,
    avatarUrl: String?,
    messageType: MessageType,
    contentType: MessageContentType
) {
    var visibilityAlpha = remember { mutableFloatStateOf(1f) }
    val threshold = with(LocalDensity.current) { 168.dp.toPx() } // 设定阈值
    val topBarHeight = with(LocalDensity.current) { 96.dp.toPx() } // 设定顶部栏高度

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
        var (avatar, messageCard) = createRefs()
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
            ) {
                when (contentType) {
                    MessageContentType.TEXT -> {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = when (messageType) {
                                MessageType.USER -> BlackText
                                MessageType.ASSISTANT, MessageType.SYSTEM -> WhiteText
                            },
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
        }
    }
}