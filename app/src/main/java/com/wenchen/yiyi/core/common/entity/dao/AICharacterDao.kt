package com.wenchen.yiyi.core.common.entity.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.wenchen.yiyi.core.common.entity.AICharacter
import kotlinx.coroutines.flow.Flow

@Dao
interface AICharacterDao {
    // 插入新角色
    @Insert
    fun insertAICharacter(character: AICharacter): Long

    // 更新角色信息
    @Update
    fun updateAICharacter(character: AICharacter): Int

    // 删除角色
    @Delete
    fun deleteAICharacter(character: AICharacter): Int

    // 根据ID查询角色
    @Query("SELECT * FROM ai_characters WHERE aiCharacterId = :id")
    fun getCharacterById(id: String): AICharacter?

    // 根据ID列表查询角色
    @Query("SELECT * FROM ai_characters WHERE aiCharacterId IN (:ids)")
    suspend fun getCharactersByIds(ids: List<String>): List<AICharacter>

    // 根据角色名称查询角色
    @Query("SELECT * FROM ai_characters WHERE name LIKE '%' || :name || '%'")
    suspend fun getCharacterByName(name: String): List<AICharacter>

    // 根据角色名称查询角色（返回PagingSource，用于分页）
    @Query("SELECT * FROM ai_characters WHERE name LIKE '%' || :name || '%' ORDER BY createdAt DESC")
    fun getCharacterByNamePagingSource(name: String): PagingSource<Int, AICharacter>

    // 查询指定用户的所有角色
    @Query("SELECT * FROM ai_characters WHERE userId = :userId ORDER BY createdAt DESC")
    fun getCharactersByUser(userId: String): Flow<List<AICharacter>>

    // 查询所有角色
    @Query("SELECT * FROM ai_characters ORDER BY createdAt DESC")
    suspend fun getAllCharacters(): List<AICharacter>

    // 查询所有角色（返回PagingSource，用于分页）
    @Query("SELECT * FROM ai_characters ORDER BY createdAt DESC")
    fun getAllCharactersPagingSource(): PagingSource<Int, AICharacter>

    // 删除指定用户的所有角色
    @Query("DELETE FROM ai_characters WHERE userId = :userId")
    suspend fun deleteAllCharactersForUser(userId: String)
}