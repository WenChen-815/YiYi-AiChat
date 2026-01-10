package com.wenchen.yiyi.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "ai_characters")
data class AICharacter(
    @PrimaryKey val aiCharacterId: String,
    val name: String,
    val roleIdentity: String, // 角色身份
    val roleAppearance: String, // 角色外观
    val roleDescription: String, // 角色描述
    val outputExample: String, // 输出示例
    val behaviorRules: String, // 行为规则
    val userId: String,
    val createdAt: Long,
    val avatarPath: String?,
    val backgroundPath: String?,
)