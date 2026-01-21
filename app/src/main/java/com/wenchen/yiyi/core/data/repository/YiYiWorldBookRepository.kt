package com.wenchen.yiyi.core.data.repository

import com.wenchen.yiyi.core.database.datasource.aiChat.YiYiWorldBookDataSource
import com.wenchen.yiyi.core.database.entity.YiYiWorldBook
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookWithEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YiYi 世界书仓库。
 */
@Singleton
class YiYiWorldBookRepository @Inject constructor(
    private val dataSource: YiYiWorldBookDataSource
) {
    suspend fun insertBook(book: YiYiWorldBook): Long {
        return dataSource.insertBook(book)
    }

    suspend fun insertBooks(books: List<YiYiWorldBook>) {
        dataSource.insertBooks(books)
    }

    suspend fun updateBook(book: YiYiWorldBook): Int {
        return dataSource.updateBook(book)
    }

    suspend fun deleteBook(book: YiYiWorldBook): Int {
        return dataSource.deleteBook(book)
    }

    suspend fun getBookById(id: String): YiYiWorldBook? {
        return dataSource.getBookById(id)
    }

    fun getAllBooksFlow(): Flow<List<YiYiWorldBook>> {
        return dataSource.getAllBooksFlow().flowOn(Dispatchers.IO)
    }

    suspend fun getAllBooks(): List<YiYiWorldBook> {
        return dataSource.getAllBooks()
    }

    suspend fun getBookWithEntriesById(id: String): YiYiWorldBookWithEntries? {
        return dataSource.getBookWithEntriesById(id)
    }

    suspend fun getBooksWithEntriesByIds(ids: List<String>): List<YiYiWorldBookWithEntries> {
        return dataSource.getBooksWithEntriesByIds(ids)
    }

    fun getAllBooksWithEntriesFlow(): Flow<List<YiYiWorldBookWithEntries>> {
        return dataSource.getAllBooksWithEntriesFlow().flowOn(Dispatchers.IO)
    }
}
