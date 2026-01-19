package com.wenchen.yiyi.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenchen.yiyi.core.database.entity.YiYiRegexScript
import kotlinx.coroutines.flow.Flow

@Dao
interface YiYiRegexScriptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: YiYiRegexScript): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScripts(scripts: List<YiYiRegexScript>)

    @Update
    suspend fun updateScript(script: YiYiRegexScript): Int

    @Delete
    suspend fun deleteScript(script: YiYiRegexScript): Int

    @Query("SELECT * FROM yiyi_regex_scripts WHERE id = :id")
    suspend fun getScriptById(id: String): YiYiRegexScript?

    @Query("SELECT * FROM yiyi_regex_scripts WHERE groupId = :groupId")
    fun getScriptsByGroupIdFlow(groupId: String): Flow<List<YiYiRegexScript>>

    @Query("SELECT * FROM yiyi_regex_scripts WHERE groupId = :groupId")
    suspend fun getScriptsByGroupId(groupId: String): List<YiYiRegexScript>

    @Query("SELECT * FROM yiyi_regex_scripts WHERE groupId IN (:groupIds)")
    suspend fun getScriptsByGroupIds(groupIds: List<String>): List<YiYiRegexScript>

    @Query("DELETE FROM yiyi_regex_scripts WHERE groupId = :groupId")
    suspend fun deleteScriptsByGroupId(groupId: String): Int
}
