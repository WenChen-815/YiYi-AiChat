package com.wenchen.yiyi.core.data.repository

import com.wenchen.yiyi.core.database.datasource.aiChat.TempChatMessageDataSource
import com.wenchen.yiyi.core.database.entity.TempChatMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 临时聊天消息仓库，暴露对 TempChatMessage 表的增删改查。
 *
 * @property tempChatMessageDataSource 临时聊天消息数据源
 */
@Singleton
class TempChatMessageRepository @Inject constructor(
    private val tempChatMessageDataSource: TempChatMessageDataSource
) {

    /**
     * 插入一条临时聊天消息。
     *
     * @param message 要插入的 TempChatMessage 实体
     * @return 新插入消息的 ID
     */
    suspend fun insert(message: TempChatMessage): Long {
        return tempChatMessageDataSource.insert(message)
    }

    /**
     * 插入多条临时聊天消息。
     *
     * @param messages 要插入的 TempChatMessage 列表
     * @return 新插入消息的 ID 列表
     */
    suspend fun insertAll(messages: List<TempChatMessage>): List<Long> {
        return tempChatMessageDataSource.insertAll(messages)
    }

    /**
     * 根据对话 ID 查询所有临时聊天记录。
     *
     * @param conversationId 对话 ID
     * @return TempChatMessage 列表
     */
    suspend fun getByConversationId(conversationId: String): List<TempChatMessage> {
        return tempChatMessageDataSource.getByConversationId(conversationId)
    }

    /**
     * 根据对话 ID 删除所有临时聊天记录。
     *
     * @param conversationId 对话 ID
     * @return 受影响的行数
     */
    suspend fun deleteByConversationId(conversationId: String): Int {
        return tempChatMessageDataSource.deleteByConversationId(conversationId)
    }

    /**
     * 根据消息 ID 删除一条消息。
     *
     * @param messageId 消息 ID
     * @return 受影响的行数
     */
    suspend fun deleteMessageById(messageId: String): Int {
        return tempChatMessageDataSource.deleteMessageById(messageId)
    }

    /**
     * 根据消息 ID 列表删除多条消息。
     *
     * @param messageIds 要删除的消息 ID 列表
     * @return 受影响的行数
     */
    suspend fun deleteMessagesByIds(messageIds: List<String>): Int {
        return tempChatMessageDataSource.deleteMessagesByIds(messageIds)
    }

    /**
     * 清空所有临时聊天记录。
     */
    suspend fun deleteAll() {
        tempChatMessageDataSource.deleteAll()
    }

    /**
     * 更新消息内容。
     *
     * @param messageId 消息 ID
     * @param content 新内容
     * @return 受影响的行数
     */
    suspend fun updateMessageContent(messageId: String, content: String): Int {
        return tempChatMessageDataSource.updateMessageContent(messageId, content)
    }
}