package com.wenchen.yiyi.core.model.network

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

// 聊天响应的数据模型
@Serializable
@Keep
data class ChatResponse(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null
)

// 聊天响应中的每个选择项
@Serializable
@Keep
data class Choice(
    val index: Int? = null,
    val message: Message? = null,
    val delta: Delta? = null, // SSE 流式返回时使用 delta 而不是 message
    val finishReason: String? = null
)

// 聊天流式增量内容
@Serializable
@Keep
data class Delta(
    val role: String? = null,
    val content: String? = null
)

// 聊天响应中的消息内容
@Serializable
@Keep
data class Message(
    val role: String? = null,
    val content: String? = null
)

// 聊天响应中的使用统计信息
@Serializable
@Keep
data class Usage(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null
)
