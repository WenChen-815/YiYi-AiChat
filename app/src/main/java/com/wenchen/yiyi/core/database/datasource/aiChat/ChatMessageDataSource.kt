package com.wenchen.yiyi.core.database.datasource.aiChat

import com.wenchen.yiyi.core.database.dao.ChatMessageDao
import com.wenchen.yiyi.core.database.entity.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天消息数据源，负责 ChatMessage 实体的数据库操作。
 *
 * @property chatMessageDao 聊天消息表的数据库访问对象
 */
@Singleton
class ChatMessageDataSource @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) {

    /**
     * 插入一条新的聊天消息。
     *
     * @param message 要插入的 ChatMessage 实体
     * @return 新插入消息的 ID
     */
    suspend fun insertMessage(message: ChatMessage): Long {
        return chatMessageDao.insertMessage(message)
    }

    /**
     * 根据对话 ID 分页获取聊天消息。
     *
     * @param conversationId 对话 ID
     * @param pageSize 每页数量
     * @param offset 偏移量
     * @return ChatMessage 列表
     */
    suspend fun getMessagesByPage(
        conversationId: String,
        pageSize: Int,
        offset: Int
    ): List<ChatMessage> {
        return chatMessageDao.getMessagesByPage(conversationId, pageSize, offset)
    }

    /**
     * 获取指定对话的消息总数。
     *
     * @param conversationId 对话 ID
     * @return 消息总数
     */
    suspend fun getTotalMessageCount(conversationId: String): Int {
        return chatMessageDao.getTotalMessageCount(conversationId)
    }

    /**
     * 获取指定对话的最后一条消息。
     *
     * @param conversationId 对话 ID
     * @return 最后一条 ChatMessage 实体
     */
    suspend fun getLastMessageByConversationId(conversationId: String): ChatMessage {
        return chatMessageDao.getLastMessageByConversationId(conversationId)
    }

    /**
     * 根据消息 ID 删除一条消息。
     *
     * @param messageId 消息 ID
     * @return 受影响的行数
     */
    suspend fun deleteMessageById(messageId: String): Int {
        return chatMessageDao.deleteMessageById(messageId)
    }

    /**
     * 根据消息 ID 列表删除多条消息。
     *
     * @param messageIds 要删除的消息 ID 列表
     * @return 受影响的行数
     */
    suspend fun deleteMessagesByIds(messageIds: List<String>): Int {
        return chatMessageDao.deleteMessagesByIds(messageIds)
    }

    /**
     * 根据对话 ID 删除所有相关的聊天消息。
     *
     * @param conversationId 对话 ID
     */
    suspend fun deleteMessagesByConversationId(conversationId: String) {
        chatMessageDao.deleteMessagesByConversationId(conversationId)
    }

    /**
     * 清空所有聊天消息。
     */
    suspend fun deleteAllMessages() {
        chatMessageDao.deleteAllMessages()
    }

    /**
     * 更新消息内容。
     *
     * @param messageId 消息 ID
     * @param content 新内容
     */
    suspend fun updateMessageContent(messageId: String, content: String) {
        chatMessageDao.updateMessageContent(messageId, content)
    }
}