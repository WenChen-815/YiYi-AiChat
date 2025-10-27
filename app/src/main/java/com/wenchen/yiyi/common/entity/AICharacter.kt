package com.wenchen.yiyi.common.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_characters")
data class AICharacter(
    @PrimaryKey val aiCharacterId: String,
    val name: String,
    val roleIdentity: String,
    val roleAppearance: String,
    val roleDescription: String,
    val outputExample: String,
    val behaviorRules: String,
    val userId: String,
    val createdAt: Long,
    val avatarPath: String?,
    val backgroundPath: String?,
)