package com.wenchen.yiyi.core.data.repository

import com.wenchen.yiyi.core.database.datasource.aiChat.ConversationDataSource
import com.wenchen.yiyi.core.database.entity.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 对话仓库，暴露对 Conversation 表的增删改查。
 *
 * @property conversationDataSource 对话数据源
 */
@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDataSource: ConversationDataSource
) {

    /**
     * 插入一个新的对话。
     *
     * @param conversation 要插入的 Conversation 实体
     * @return 新插入对话的 ID
     */
    suspend fun insert(conversation: Conversation): Long {
        return conversationDataSource.insert(conversation)
    }

    /**
     * 更新一个已有的对话信息。
     *
     * @param conversation 包含更新信息的 Conversation 实体
     * @return 受影响的行数
     */
    suspend fun update(conversation: Conversation): Int {
        return conversationDataSource.update(conversation)
    }

    /**
     * 根据 ID 获取对话。
     *
     * @param conversationId 对话 ID
     * @return 对应的 Conversation 实体，如果不存在则返回 null
     */
    suspend fun getById(conversationId: String): Conversation? {
        return conversationDataSource.getById(conversationId)
    }

    /**
     * 获取所有对话列表。
     *
     * @return 包含所有 Conversation 的列表
     */
    suspend fun getAllConversations(): List<Conversation> {
        return conversationDataSource.getAllConversations()
    }

    /**
     * 根据角色 ID 获取其所有对话。
     *
     * @param characterId 角色 ID
     * @return 该角色下的所有 Conversation 列表
     */
    suspend fun getByCharacterId(characterId: String): List<Conversation> {
        return conversationDataSource.getByCharacterId(characterId)
    }

    /**
     * 根据 ID 删除一个对话。
     *
     * @param conversationId 要删除的对话 ID
     * @return 受影响的行数
     */
    suspend fun deleteById(conversationId: String): Int {
        return conversationDataSource.deleteById(conversationId)
    }

    /**
     * 根据角色 ID 删除其所有对话。
     *
     * @param characterId 角色 ID
     */
    suspend fun deleteByCharacterId(characterId: String) {
        conversationDataSource.deleteByCharacterId(characterId)
    }
}
