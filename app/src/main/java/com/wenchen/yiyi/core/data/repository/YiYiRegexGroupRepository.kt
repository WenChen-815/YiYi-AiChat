package com.wenchen.yiyi.core.data.repository

import com.wenchen.yiyi.core.database.datasource.aiChat.YiYiRegexGroupDataSource
import com.wenchen.yiyi.core.database.entity.YiYiRegexGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YiYi 正则分组仓库。
 */
@Singleton
class YiYiRegexGroupRepository @Inject constructor(
    private val dataSource: YiYiRegexGroupDataSource
) {
    suspend fun insertGroup(group: YiYiRegexGroup): Long {
        return dataSource.insertGroup(group)
    }

    suspend fun insertGroups(groups: List<YiYiRegexGroup>) {
        dataSource.insertGroups(groups)
    }

    suspend fun updateGroup(group: YiYiRegexGroup): Int {
        return dataSource.updateGroup(group)
    }

    suspend fun deleteGroup(group: YiYiRegexGroup): Int {
        return dataSource.deleteGroup(group)
    }

    suspend fun getGroupById(id: String): YiYiRegexGroup? {
        return dataSource.getGroupById(id)
    }

    fun getAllGroupsFlow(): Flow<List<YiYiRegexGroup>> {
        return dataSource.getAllGroupsFlow().flowOn(Dispatchers.IO)
    }

    suspend fun getAllGroups(): List<YiYiRegexGroup> {
        return dataSource.getAllGroups()
    }
}
