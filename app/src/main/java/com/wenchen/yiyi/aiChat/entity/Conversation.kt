package com.wenchen.yiyi.aiChat.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ConversationType {
    SINGLE, GROUP
}

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    var name: String,
    val type: ConversationType,
    val characterIds: Map<String, Float>,

    var playerName: String,
    var playGender: String,
    var playerDescription: String,

    var chatWorldId: String,
    var chatSceneDescription: String,

    val avatarPath: String?,
    val backgroundPath: String?,
)