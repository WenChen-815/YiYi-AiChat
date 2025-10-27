package com.wenchen.yiyi.aiChat.entity.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.wenchen.yiyi.aiChat.entity.Conversation

@Dao
interface ConversationDao {
    @Insert
    suspend fun insert(conversation: Conversation): Long

    @Update
    suspend fun update(conversation: Conversation): Int

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getById(conversationId: String): Conversation?

    @Query("SELECT * FROM conversations")
    suspend fun getAllConversations(): List<Conversation>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getByCharacterId(id: String): List<Conversation>

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteById(conversationId: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteByCharacterId(id: String)
}
