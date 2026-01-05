package com.wenchen.yiyi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wenchen.yiyi.core.database.entity.ChatMessage

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :pageSize OFFSET :offset")
    suspend fun getMessagesByPage(conversationId: String, pageSize: Int, offset: Int): List<ChatMessage>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun getTotalMessageCount(conversationId: String): Int

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageByConversationId(conversationId: String): ChatMessage

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String): Int

    @Query("DELETE FROM chat_messages WHERE id IN (:messageIds)")
    suspend fun deleteMessagesByIds(messageIds: List<String>): Int

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversationId(conversationId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    @Query("UPDATE chat_messages SET content = :content WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, content: String)
}