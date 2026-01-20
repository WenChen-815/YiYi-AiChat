package com.wenchen.yiyi.core.database.datasource.aiChat

import com.wenchen.yiyi.core.database.dao.YiYiWorldBookEntryDao
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YiYiWorldBookEntryDataSource @Inject constructor(
    private val yiYiWorldBookEntryDao: YiYiWorldBookEntryDao
) {
    suspend fun insertEntry(entry: YiYiWorldBookEntry): Long {
        return yiYiWorldBookEntryDao.insertEntry(entry)
    }

    suspend fun insertEntries(entries: List<YiYiWorldBookEntry>) {
        yiYiWorldBookEntryDao.insertEntries(entries)
    }

    suspend fun updateEntry(entry: YiYiWorldBookEntry): Int {
        return yiYiWorldBookEntryDao.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: YiYiWorldBookEntry): Int {
        return yiYiWorldBookEntryDao.deleteEntry(entry)
    }

    suspend fun getEntryById(id: String): YiYiWorldBookEntry? {
        return yiYiWorldBookEntryDao.getEntryById(id)
    }

    fun getEntriesByBookIdFlow(bookId: String): Flow<List<YiYiWorldBookEntry>> {
        return yiYiWorldBookEntryDao.getEntriesByBookIdFlow(bookId)
    }

    suspend fun getEntriesByBookId(bookId: String): List<YiYiWorldBookEntry> {
        return yiYiWorldBookEntryDao.getEntriesByBookId(bookId)
    }

    suspend fun deleteEntriesByBookId(bookId: String): Int {
        return yiYiWorldBookEntryDao.deleteEntriesByBookId(bookId)
    }
}
