package com.wenchen.yiyi.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenchen.yiyi.core.database.entity.YiYiRegexGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface YiYiRegexGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: YiYiRegexGroup): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<YiYiRegexGroup>)

    @Update
    suspend fun updateGroup(group: YiYiRegexGroup): Int

    @Delete
    suspend fun deleteGroup(group: YiYiRegexGroup): Int

    @Query("SELECT * FROM yiyi_regex_groups WHERE id = :id")
    suspend fun getGroupById(id: String): YiYiRegexGroup?

    @Query("SELECT * FROM yiyi_regex_groups ORDER BY updateTime DESC")
    fun getAllGroupsFlow(): Flow<List<YiYiRegexGroup>>

    @Query("SELECT * FROM yiyi_regex_groups ORDER BY updateTime DESC")
    suspend fun getAllGroups(): List<YiYiRegexGroup>
}
