package com.wenchen.yiyi.core.model.network

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

// 聊天响应的数据模型
@Serializable
@Keep
data class ChatResponse(
    val id: String?,
    val `object`: String?,
    val created: Long?,
    val choices: List<Choice>?,
    val usage: Usage?
)

// 聊天响应中的每个选择项
@Serializable
@Keep
data class Choice(
    val index: Int?,
    val message: Message?,
    val finishReason: String?
)

// 聊天响应中的消息内容
@Serializable
@Keep
data class Message(
    val role: String?,
    val content: String?
)

// 聊天响应中的使用统计信息
@Serializable
@Keep
data class Usage(
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)
