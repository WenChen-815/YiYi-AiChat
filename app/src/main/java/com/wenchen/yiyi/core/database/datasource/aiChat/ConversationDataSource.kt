package com.wenchen.yiyi.core.database.datasource.aiChat

import com.wenchen.yiyi.core.database.dao.ConversationDao
import com.wenchen.yiyi.core.database.entity.Conversation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 对话数据源，负责 Conversation 实体的数据库操作。
 *
 * @property conversationDao 对话表的数据库访问对象
 */
@Singleton
class ConversationDataSource @Inject constructor(
    private val conversationDao: ConversationDao
) {

    /**
     * 插入一个新的对话。
     *
     * @param conversation 要插入的 Conversation 实体
     * @return 新插入对话的 ID
     */
    suspend fun insert(conversation: Conversation): Long {
        return conversationDao.insert(conversation)
    }

    /**
     * 更新一个已有的对话信息。
     *
     * @param conversation 包含更新信息的 Conversation 实体
     * @return 受影响的行数
     */
    suspend fun update(conversation: Conversation): Int {
        return conversationDao.update(conversation)
    }

    /**
     * 根据 ID 获取对话。
     *
     * @param conversationId 对话 ID
     * @return 对应的 Conversation 实体，如果不存在则返回 null
     */
    suspend fun getById(conversationId: String): Conversation? {
        return conversationDao.getById(conversationId)
    }

    /**
     * 获取所有对话列表。
     *
     * @return 包含所有 Conversation 的列表
     */
    suspend fun getAllConversations(): List<Conversation> {
        return conversationDao.getAllConversations()
    }

    /**
     * 根据角色 ID 获取其所有对话。
     *
     * @param characterId 角色 ID
     * @return 该角色下的所有 Conversation 列表
     */
    suspend fun getByCharacterId(characterId: String): List<Conversation> {
        return conversationDao.getByCharacterId(characterId)
    }

    /**
     * 根据 ID 删除一个对话。
     *
     * @param conversationId 要删除的对话 ID
     * @return 受影响的行数
     */
    suspend fun deleteById(conversationId: String): Int {
        return conversationDao.deleteById(conversationId)
    }

    /**
     * 根据角色 ID 删除其所有对话。
     *
     * @param characterId 角色 ID
     */
    suspend fun deleteByCharacterId(characterId: String) {
        conversationDao.deleteByCharacterId(characterId)
    }
}