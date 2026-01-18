package com.wenchen.yiyi.core.database.datasource.aiChat

import com.wenchen.yiyi.core.database.dao.YiYiRegexGroupDao
import com.wenchen.yiyi.core.database.entity.YiYiRegexGroup
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YiYiRegexGroupDataSource @Inject constructor(
    private val yiYiRegexGroupDao: YiYiRegexGroupDao
) {
    suspend fun insertGroup(group: YiYiRegexGroup): Long {
        return yiYiRegexGroupDao.insertGroup(group)
    }

    suspend fun insertGroups(groups: List<YiYiRegexGroup>) {
        yiYiRegexGroupDao.insertGroups(groups)
    }

    suspend fun updateGroup(group: YiYiRegexGroup): Int {
        return yiYiRegexGroupDao.updateGroup(group)
    }

    suspend fun deleteGroup(group: YiYiRegexGroup): Int {
        return yiYiRegexGroupDao.deleteGroup(group)
    }

    suspend fun getGroupById(id: String): YiYiRegexGroup? {
        return yiYiRegexGroupDao.getGroupById(id)
    }

    fun getAllGroupsFlow(): Flow<List<YiYiRegexGroup>> {
        return yiYiRegexGroupDao.getAllGroupsFlow()
    }

    suspend fun getAllGroups(): List<YiYiRegexGroup> {
        return yiYiRegexGroupDao.getAllGroups()
    }
}
