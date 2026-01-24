package com.wenchen.yiyi.feature.aiChat.common

import androidx.compose.ui.graphics.Color
import com.wenchen.yiyi.core.designSystem.theme.BgGreyDark
import com.wenchen.yiyi.core.designSystem.theme.PrimaryLight
import com.wenchen.yiyi.core.designSystem.theme.TextPrimaryLight
import com.wenchen.yiyi.core.designSystem.theme.TextWhite
import com.wenchen.yiyi.core.util.common.Serializer
import kotlinx.serialization.Serializable

@Serializable
enum class LayoutType { STANDARD, ONLY_BUBBLE, FULL, FULL_EXTENDED }

@Serializable
data class ChatLayoutMode(
    val type: LayoutType = LayoutType.STANDARD,
    @Serializable(with = Serializer::class) val userBubbleColor: Color = PrimaryLight,
    @Serializable(with = Serializer::class) val userTextColor: Color = TextPrimaryLight,
    @Serializable(with = Serializer::class) val aiBubbleColor: Color = BgGreyDark,
    @Serializable(with = Serializer::class) val aiTextColor: Color = TextWhite
)
