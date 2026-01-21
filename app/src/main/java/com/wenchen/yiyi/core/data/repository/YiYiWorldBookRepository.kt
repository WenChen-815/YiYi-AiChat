package com.wenchen.yiyi.core.data.repository

import androidx.room.withTransaction
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.database.AppDatabase
import com.wenchen.yiyi.core.database.datasource.aiChat.YiYiWorldBookDataSource
import com.wenchen.yiyi.core.database.datasource.aiChat.YiYiWorldBookEntryDataSource
import com.wenchen.yiyi.core.database.entity.YiYiWorldBook
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookEntry
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
    private val bookDataSource: YiYiWorldBookDataSource,
    private val entryDataSource: YiYiWorldBookEntryDataSource,
) {
    suspend fun insertBook(book: YiYiWorldBook): Long {
        return bookDataSource.insertBook(book)
    }

    suspend fun insertBooks(books: List<YiYiWorldBook>) {
        bookDataSource.insertBooks(books)
    }

    suspend fun updateBook(book: YiYiWorldBook): Int {
        return bookDataSource.updateBook(book)
    }

    suspend fun deleteBook(book: YiYiWorldBook): Int {
        return bookDataSource.deleteBook(book)
    }

    suspend fun getBookById(id: String): YiYiWorldBook? {
        return bookDataSource.getBookById(id)
    }

    fun getAllBooksFlow(): Flow<List<YiYiWorldBook>> {
        return bookDataSource.getAllBooksFlow().flowOn(Dispatchers.IO)
    }

    suspend fun getAllBooks(): List<YiYiWorldBook> {
        return bookDataSource.getAllBooks()
    }

    suspend fun getBookWithEntriesById(id: String): YiYiWorldBookWithEntries? {
        return bookDataSource.getBookWithEntriesById(id)
    }

    suspend fun getBooksWithEntriesByIds(ids: List<String>): List<YiYiWorldBookWithEntries> {
        return bookDataSource.getBooksWithEntriesByIds(ids)
    }

    fun getAllBooksWithEntriesFlow(): Flow<List<YiYiWorldBookWithEntries>> {
        return bookDataSource.getAllBooksWithEntriesFlow().flowOn(Dispatchers.IO)
    }

    suspend fun saveBookWithEntries(book: YiYiWorldBook, entries: List<YiYiWorldBookEntry>) {
        Application.appDatabase.withTransaction {
            bookDataSource.insertBook(book)
            entryDataSource.insertEntries(entries)
        }
    }
}
