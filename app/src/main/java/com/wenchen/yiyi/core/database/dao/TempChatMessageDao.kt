package com.wenchen.yiyi.core.database.dao

import androidx.room.*
import com.wenchen.yiyi.core.database.entity.TempChatMessage

@Dao
interface TempChatMessageDao {
    // 插入单个临时聊天消息
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: TempChatMessage): Long

    // 插入多个临时聊天消息
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<TempChatMessage>): List<Long>

    // 更新临时聊天消息
    @Update
    suspend fun update(message: TempChatMessage): Int

    // 删除单个临时聊天消息
    @Delete
    suspend fun delete(message: TempChatMessage): Int

    // 删除多个临时聊天消息
    @Delete
    suspend fun deleteAll(messages: List<TempChatMessage>): Int

    // 根据ID查询临时聊天消息
    @Query("SELECT * FROM temp_chat_messages WHERE id = :id")
    suspend fun getById(id: String): TempChatMessage?

    // 根据角色ID查询所有临时聊天记录
    @Query("SELECT * FROM temp_chat_messages WHERE characterId = :characterId ORDER BY timestamp ASC")
    suspend fun getByCharacterId(characterId: String): List<TempChatMessage>

    // 根据用户ID查询所有临时聊天记录
    @Query("SELECT * FROM temp_chat_messages WHERE chatUserId = :chatUserId ORDER BY timestamp ASC")
    suspend fun getByUserId(chatUserId: String): List<TempChatMessage>

    // 根据对话ID查询所有临时聊天记录
    @Query("SELECT * FROM temp_chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getByConversationId(conversationId: String): List<TempChatMessage>

    // 删除指定角色的所有临时聊天记录
    @Query("DELETE FROM temp_chat_messages WHERE characterId = :characterId")
    suspend fun deleteByCharacterId(characterId: String): Int

    // 删除指定对话的所有临时聊天记录
    @Query("DELETE FROM temp_chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversationId(conversationId: String): Int

    // 删除指定用户的所有临时聊天记录
    @Query("DELETE FROM temp_chat_messages WHERE chatUserId = :chatUserId")
    suspend fun deleteByUserId(chatUserId: String): Int

    // 清空所有临时聊天记录
    @Query("DELETE FROM temp_chat_messages")
    suspend fun deleteAll(): Int

    // 根据角色ID获取临时聊天记录的数量
    @Query("SELECT COUNT(*) FROM temp_chat_messages WHERE characterId = :characterId")
    suspend fun getCountByCharacterId(characterId: String): Int

    // 根据对话ID获取临时聊天记录的数量
    @Query("SELECT COUNT(*) FROM temp_chat_messages WHERE conversationId = :conversationId")
    suspend fun getCountByConversationId(conversationId: String): Int

    // 更新消息内容
    @Query("UPDATE temp_chat_messages SET content = :content WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, content: String): Int

    // 删除指定消息
    @Query("DELETE FROM temp_chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String): Int

     // 删除指定消息列表
    @Query("DELETE FROM temp_chat_messages WHERE id IN (:messageIds)")
    suspend fun deleteMessagesByIds(messageIds: List<String>): Int
}
