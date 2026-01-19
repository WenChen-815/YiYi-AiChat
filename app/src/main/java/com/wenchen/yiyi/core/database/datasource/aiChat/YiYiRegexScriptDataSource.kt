package com.wenchen.yiyi.core.database.datasource.aiChat

import com.wenchen.yiyi.core.database.dao.YiYiRegexScriptDao
import com.wenchen.yiyi.core.database.entity.YiYiRegexScript
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YiYiRegexScriptDataSource @Inject constructor(
    private val yiYiRegexScriptDao: YiYiRegexScriptDao
) {
    suspend fun insertScript(script: YiYiRegexScript): Long {
        return yiYiRegexScriptDao.insertScript(script)
    }

    suspend fun insertScripts(scripts: List<YiYiRegexScript>) {
        yiYiRegexScriptDao.insertScripts(scripts)
    }

    suspend fun updateScript(script: YiYiRegexScript): Int {
        return yiYiRegexScriptDao.updateScript(script)
    }

    suspend fun deleteScript(script: YiYiRegexScript): Int {
        return yiYiRegexScriptDao.deleteScript(script)
    }

    suspend fun getScriptById(id: String): YiYiRegexScript? {
        return yiYiRegexScriptDao.getScriptById(id)
    }

    fun getScriptsByGroupIdFlow(groupId: String): Flow<List<YiYiRegexScript>> {
        return yiYiRegexScriptDao.getScriptsByGroupIdFlow(groupId)
    }

    suspend fun getScriptsByGroupId(groupId: String): List<YiYiRegexScript> {
        return yiYiRegexScriptDao.getScriptsByGroupId(groupId)
    }

    suspend fun getScriptsByGroupIds(groupIds: List<String>): List<YiYiRegexScript> {
        return yiYiRegexScriptDao.getScriptsByGroupIds(groupIds)
    }

    suspend fun deleteScriptsByGroupId(groupId: String): Int {
        return yiYiRegexScriptDao.deleteScriptsByGroupId(groupId)
    }
}
