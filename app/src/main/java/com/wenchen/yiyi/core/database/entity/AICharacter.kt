package com.wenchen.yiyi.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "ai_characters")
data class AICharacter(
    @PrimaryKey val id: String,
    val name: String,
    val userId: String,
    val description: String, // 融合后的提示词
    val first_mes: String?, // 初始消息
    val mes_example: String?, // 输出示例
    val personality: String?, // 性格
    val scenario: String?, // 场景
    val creator_notes: String?, // 笔记
    val system_prompt: String?, // 系统提示
    val post_history_instructions: String?, // 历史指令
    val avatar: String?,
    val background: String?,
    val creation_date: Long,
    val modification_date: Long,
)