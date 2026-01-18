package com.wenchen.yiyi.core.data.repository

import com.wenchen.yiyi.core.database.datasource.aiChat.YiYiRegexScriptDataSource
import com.wenchen.yiyi.core.database.entity.YiYiRegexScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YiYi 正则脚本仓库。
 */
@Singleton
class YiYiRegexScriptRepository @Inject constructor(
    private val dataSource: YiYiRegexScriptDataSource
) {
    suspend fun insertScript(script: YiYiRegexScript): Long {
        return dataSource.insertScript(script)
    }

    suspend fun insertScripts(scripts: List<YiYiRegexScript>) {
        dataSource.insertScripts(scripts)
    }

    suspend fun updateScript(script: YiYiRegexScript): Int {
        return dataSource.updateScript(script)
    }

    suspend fun deleteScript(script: YiYiRegexScript): Int {
        return dataSource.deleteScript(script)
    }

    suspend fun getScriptById(id: String): YiYiRegexScript? {
        return dataSource.getScriptById(id)
    }

    fun getScriptsByGroupIdFlow(groupId: String): Flow<List<YiYiRegexScript>> {
        return dataSource.getScriptsByGroupIdFlow(groupId).flowOn(Dispatchers.IO)
    }

    suspend fun getScriptsByGroupId(groupId: String): List<YiYiRegexScript> {
        return dataSource.getScriptsByGroupId(groupId)
    }

    suspend fun deleteScriptsByGroupId(groupId: String): Int {
        return dataSource.deleteScriptsByGroupId(groupId)
    }
}
