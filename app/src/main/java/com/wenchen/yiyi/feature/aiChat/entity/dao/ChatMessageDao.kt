package com.wenchen.yiyi.feature.aiChat.entity.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wenchen.yiyi.feature.aiChat.entity.ChatMessage

@Dao
interface ChatMessageDao {
    // 插入新消息
    @Insert
    suspend fun insertMessage(message: ChatMessage): Long

    // 根据角色ID查询所有聊天记录
    @Query("SELECT * FROM chat_messages WHERE characterId = :characterId ORDER BY timestamp ASC")
    fun getMessagesByCharacterId(characterId: String): List<ChatMessage>

    // 分页查询，按时间倒序排列（最新的在前面）
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :pageSize OFFSET :offset")
    suspend fun getMessagesByPage(conversationId: String, pageSize: Int, offset: Int): List<ChatMessage>

    // 获取最近一条消息
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageByConversationId(conversationId: String): ChatMessage

    // 获取消息总数，用于判断是否还有更多数据
    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getTotalMessageCount(): Int

    // 删除指定角色的所有聊天记录
    @Query("DELETE FROM chat_messages WHERE characterId = :characterId")
    suspend fun deleteMessagesByCharacterId(characterId: String)

    // 删除指定对话的所有聊天记录
    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversationId(conversationId: String)

    // 清空所有聊天记录
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    // 更新消息内容
    @Query("UPDATE chat_messages SET content = :content WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, content: String): Int

    // 删除指定消息
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String): Int

    // 删除指定消息列表
    @Query("DELETE FROM chat_messages WHERE id IN (:messageIds)")
    suspend fun deleteMessagesByIds(messageIds: List<String>): Int
}