package com.wenchen.yiyi.core.data.repository

import androidx.paging.PagingSource
import com.wenchen.yiyi.core.database.datasource.aiChat.AICharacterDataSource
import com.wenchen.yiyi.core.database.entity.AICharacter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 角色仓库，暴露对 AICharacter 表的增删改查。
 *
 * @property aiCharacterDataSource AI 角色数据源
 */
@Singleton
class AICharacterRepository @Inject constructor(
    private val aiCharacterDataSource: AICharacterDataSource
) {

    /**
     * 插入一个新的 AI 角色。
     *
     * @param character 要插入的 AICharacter 实体
     * @return 新插入角色的 ID
     */
    suspend fun insertAICharacter(character: AICharacter): Long {
        return aiCharacterDataSource.insertAICharacter(character)
    }

    /**
     * 更新一个已有的 AI 角色信息。
     *
     * @param character 包含更新信息的 AICharacter 实体
     * @return 受影响的行数
     */
    suspend fun updateAICharacter(character: AICharacter): Int {
        return aiCharacterDataSource.updateAICharacter(character)
    }

    /**
     * 删除一个 AI 角色。
     *
     * @param character 要删除的 AICharacter 实体
     * @return 受影响的行数
     */
    suspend fun deleteAICharacter(character: AICharacter): Int {
        return aiCharacterDataSource.deleteAICharacter(character)
    }

    /**
     * 根据 ID 查询 AI 角色。
     *
     * @param id 角色 ID
     * @return 对应的 AICharacter 实体，如果不存在则返回 null
     */
    suspend fun getCharacterById(id: String): AICharacter? {
        return aiCharacterDataSource.getCharacterById(id)
    }

    /**
     * 根据名称模糊查询 AI 角色列表。
     *
     * @param name 角色名称
     * @return 匹配的 AICharacter 列表
     */
    suspend fun getCharacterByName(name: String): List<AICharacter> {
        return aiCharacterDataSource.getCharacterByName(name)
    }

    /**
     * 监听指定用户的所有 AI 角色列表。
     *
     * @param userId 用户 ID
     * @return AICharacter 列表的 Flow
     */
    fun getCharactersByUser(userId: String): Flow<List<AICharacter>> {
        return aiCharacterDataSource.getCharactersByUser(userId).flowOn(Dispatchers.IO)
    }

    /**
     * 获取所有 AI 角色列表。
     *
     * @return 包含所有 AICharacter 的列表
     */
    suspend fun getAllCharacters(): List<AICharacter> {
        return aiCharacterDataSource.getAllCharacters()
    }

    /**
     * 获取用于分页的 AI 角色数据源。
     *
     * @return PagingSource<Int, AICharacter>
     */
    fun getAllCharactersPagingSource(): PagingSource<Int, AICharacter> {
        return aiCharacterDataSource.getAllCharactersPagingSource()
    }

    /**
     * 删除指定用户的所有 AI 角色。
     *
     * @param userId 用户 ID
     */
    suspend fun deleteAllCharactersForUser(userId: String) {
        aiCharacterDataSource.deleteAllCharactersForUser(userId)
    }
}
