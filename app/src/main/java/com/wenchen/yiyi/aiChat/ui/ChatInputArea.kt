package com.wenchen.yiyi.aiChat.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.wenchen.yiyi.aiChat.vm.ChatViewModel
import com.wenchen.yiyi.common.components.YiYiTextField
import com.wenchen.yiyi.common.theme.*

@Composable
fun ChatInputArea(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onPickImage: () -> Unit,
    onSendSysMsgClick: () -> Unit,
    isSendSystemMessage: Boolean,
    isAiReplying: Boolean,
    themeBgColor: Color = if (isSystemInDarkTheme()) BlackBg else WhiteBg,
) {
    val uiState by viewModel.uiState.collectAsState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val offset by infiniteTransition.animateFloat(
            initialValue = -100f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            // 系统消息切换按钮
            Button(
                onClick = onSendSysMsgClick,
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 8.dp)
                    .border(
                        width = 1.dp,
                        brush = if (isSendSystemMessage) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White,
                                    Color.Transparent,
                                ),
                                start = Offset(offset, offset),
                                end = Offset(offset + 100f, offset + 100f)
                            )
                        } else {
                            SolidColor(Color.Transparent)
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .height(24.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(vertical = 2.dp, horizontal = 3.dp)
            ) {
                Text(
                    text = "系统消息",
                    color = Color.White
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(themeBgColor)
                .background(HalfTransparentWhite)
                .padding(5.dp, 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            YiYiTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "向${uiState.currentCharacter?.name ?: "[未选择角色]"}发送消息",
                        style = MaterialTheme.typography.bodyLarge.copy(color = WhiteText.copy(0.6f))
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,    // 获得焦点时的下划线颜色
                    unfocusedIndicatorColor = Color.Transparent,  // 失去焦点时的下划线颜色
                    disabledIndicatorColor = Color.Transparent,   // 禁用时的下划线颜色
//                    cursorColor = WhiteText,  // 获得焦点时的光标颜色
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = WhiteText),
                maxLines = 4,
                minLines = 1,
                contentPadding = PaddingValues(3.dp, 2.dp)
            )

            IconButton(
                onClick = if (messageText.isNotBlank()) onSendMessage else onPickImage,
                enabled = !isAiReplying,
            ) {
                if (messageText.isNotBlank()) {
                    Icon(
                        Icons.Rounded.ArrowUpward,
                        contentDescription = "发送",
                        tint = WhiteText,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Gold)
                            .padding(4.dp)
                    )
                } else {
                    Icon(
                        Icons.Rounded.AddCircleOutline,
                        contentDescription = "发送图片",
                        tint = WhiteText
                    )
                }
            }
        }
    }
}