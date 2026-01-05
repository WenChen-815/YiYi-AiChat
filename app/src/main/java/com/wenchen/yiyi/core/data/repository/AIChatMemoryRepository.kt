package com.wenchen.yiyi.core.data.repository

import com.wenchen.yiyi.core.database.datasource.aiChat.AIChatMemoryDataSource
import com.wenchen.yiyi.core.database.entity.AIChatMemory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 聊天记忆仓库，暴露对 AIChatMemory 表的增删改查。
 *
 * @property aiChatMemoryDataSource AI 聊天记忆数据源
 */
@Singleton
class AIChatMemoryRepository @Inject constructor(
    private val aiChatMemoryDataSource: AIChatMemoryDataSource
) {
    /**
     * 插入或替换一条 AI 聊天记忆。
     *
     * @param memory 要插入的 AIChatMemory 实体
     * @return 新插入记忆的 ID
     */
    suspend fun insert(memory: AIChatMemory): Long {
        return aiChatMemoryDataSource.insert(memory)
    }

    /**
     * 更新一条 AI 聊天记忆。
     *
     * @param memory 包含更新信息的 AIChatMemory 实体
     * @return 受影响的行数
     */
    suspend fun update(memory: AIChatMemory): Int {
        return aiChatMemoryDataSource.update(memory)
    }

    /**
     * 根据角色ID和对话ID获取聊天记忆。
     *
     * @param characterId 角色 ID
     * @param conversationId 对话 ID
     * @return 对应的 AIChatMemory 实体，如果不存在则返回 null
     */
    suspend fun getByCharacterIdAndConversationId(
        characterId: String,
        conversationId: String
    ): AIChatMemory? {
        return aiChatMemoryDataSource.getByCharacterIdAndConversationId(characterId, conversationId)
    }

    /**
     * 根据对话 ID 删除相关的所有聊天记忆。
     *
     * @param conversationId 对话 ID
     * @return 受影响的行数
     */
    suspend fun deleteByConversationId(conversationId: String): Int {
        return aiChatMemoryDataSource.deleteByConversationId(conversationId)
    }

    /**
     * 根据角色ID和对话ID删除聊天记忆。
     *
     * @param characterId 角色 ID
     * @param conversationId 对话 ID
     * @return 受影响的行数
     */
    suspend fun deleteByCharacterIdAndConversationId(
        characterId: String,
        conversationId: String
    ): Int {
        return aiChatMemoryDataSource.deleteByCharacterIdAndConversationId(characterId, conversationId)
    }
}
