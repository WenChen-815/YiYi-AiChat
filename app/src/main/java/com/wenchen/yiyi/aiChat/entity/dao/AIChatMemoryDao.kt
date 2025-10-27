package com.wenchen.yiyi.aiChat.entity.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenchen.yiyi.aiChat.entity.AIChatMemory

@Dao
interface AIChatMemoryDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(memory: AIChatMemory): Long

    @Update
    suspend fun update(memory: AIChatMemory): Int

    @Delete
    suspend fun delete(memory: AIChatMemory): Int

    @Query("SELECT * FROM ai_chat_memory WHERE characterId = :characterId AND conversationId = :conversationId")
    suspend fun getByCharacterIdAndConversationId(
        characterId: String,
        conversationId: String
    ): AIChatMemory?

    @Query("SELECT * FROM ai_chat_memory")
    suspend fun getAll(): List<AIChatMemory>

    @Query("DELETE FROM ai_chat_memory WHERE characterId = :characterId")
    suspend fun deleteByCharacterId(characterId: String): Int

    @Query("DELETE FROM ai_chat_memory WHERE conversationId = :conversationId")
    suspend fun deleteByConversationId(conversationId: String): Int

    @Query("DELETE FROM ai_chat_memory WHERE characterId = :characterId AND conversationId = :conversationId")
    suspend fun deleteByCharacterIdAndConversationId(
        characterId: String,
        conversationId: String
    ): Int

    @Query("SELECT COUNT(*) FROM ai_chat_memory")
    suspend fun getCount(): Int
}