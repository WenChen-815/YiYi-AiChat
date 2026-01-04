package com.wenchen.yiyi.core.database.entity

// 聊天响应的数据模型
data class ChatResponse(
    val id: String?,
    val `object`: String?,
    val created: Long?,
    val choices: List<Choice>?,
    val usage: Usage?
)

data class Choice(
    val index: Int?,
    val message: Message?,
    val finishReason: String?
)

data class Message(
    val role: String?,
    val content: String?
)

data class Usage(
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)
