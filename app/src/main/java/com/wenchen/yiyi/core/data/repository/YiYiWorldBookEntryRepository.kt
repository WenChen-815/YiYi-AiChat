package com.wenchen.yiyi.core.data.repository

import com.wenchen.yiyi.core.database.datasource.aiChat.YiYiWorldBookEntryDataSource
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YiYi 世界书条目仓库。
 */
@Singleton
class YiYiWorldBookEntryRepository @Inject constructor(
    private val dataSource: YiYiWorldBookEntryDataSource
) {
    suspend fun insertEntry(entry: YiYiWorldBookEntry): Long {
        return dataSource.insertEntry(entry)
    }

    suspend fun insertEntries(entries: List<YiYiWorldBookEntry>) {
        dataSource.insertEntries(entries)
    }

    suspend fun updateEntry(entry: YiYiWorldBookEntry): Int {
        return dataSource.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: YiYiWorldBookEntry): Int {
        return dataSource.deleteEntry(entry)
    }

    suspend fun getEntryById(id: String): YiYiWorldBookEntry? {
        return dataSource.getEntryById(id)
    }

    fun getEntriesByBookIdFlow(bookId: String): Flow<List<YiYiWorldBookEntry>> {
        return dataSource.getEntriesByBookIdFlow(bookId).flowOn(Dispatchers.IO)
    }

    suspend fun getEntriesByBookId(bookId: String): List<YiYiWorldBookEntry> {
        return dataSource.getEntriesByBookId(bookId)
    }

    suspend fun deleteEntriesByBookId(bookId: String): Int {
        return dataSource.deleteEntriesByBookId(bookId)
    }
}
