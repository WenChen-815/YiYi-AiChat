package com.wenchen.yiyi.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class ConversationType {
    SINGLE, GROUP
}

@Serializable
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    var name: String,
    val type: ConversationType,

    val characterIds: Map<String, Float>,
    /*
    知识点：数据库迁移
    这里设置为可空是为了兼容旧版本数据库，旧版本数据库中没有 characterKeywords 列，迁移到新版后 characterKeywords 为 NULL
    如果这里设置为 val characterKeywords: Map<String, List<String>>
    那么数据库迁移时会失败，因为旧版本数据库该字段为NULL，而新版本要求NOT NULL，产生冲突
     */
    val characterKeywords: Map<String, List<String>>? = null,

    var playerName: String,
    var playGender: String,
    var playerDescription: String,

    var chatSceneDescription: String,
    var additionalSummaryRequirement: String? = null,
    val avatarPath: String?,
    val backgroundPath: String?,

    var chatWorldId: List<String> = emptyList(),
    val enabledRegexGroups: List<String>? = null,
)