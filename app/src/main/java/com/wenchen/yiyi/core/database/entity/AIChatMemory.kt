package com.wenchen.yiyi.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_memory")
data class AIChatMemory(
    @PrimaryKey val id: String,
    val conversationId: String,
    val characterId: String,
    var content: String = "",
    var count: Int = 0,
    val createdAt: Long,
)