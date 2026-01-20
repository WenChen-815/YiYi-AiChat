package com.wenchen.yiyi.core.database.datasource.aiChat

import com.wenchen.yiyi.core.database.dao.YiYiWorldBookDao
import com.wenchen.yiyi.core.database.entity.YiYiWorldBook
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookWithEntries
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YiYiWorldBookDataSource @Inject constructor(
    private val yiYiWorldBookDao: YiYiWorldBookDao
) {
    suspend fun insertBook(book: YiYiWorldBook): Long {
        return yiYiWorldBookDao.insertBook(book)
    }

    suspend fun insertBooks(books: List<YiYiWorldBook>) {
        yiYiWorldBookDao.insertBooks(books)
    }

    suspend fun updateBook(book: YiYiWorldBook): Int {
        return yiYiWorldBookDao.updateBook(book)
    }

    suspend fun deleteBook(book: YiYiWorldBook): Int {
        return yiYiWorldBookDao.deleteBook(book)
    }

    suspend fun getBookById(id: String): YiYiWorldBook? {
        return yiYiWorldBookDao.getBookById(id)
    }

    fun getAllBooksFlow(): Flow<List<YiYiWorldBook>> {
        return yiYiWorldBookDao.getAllBooksFlow()
    }

    suspend fun getAllBooks(): List<YiYiWorldBook> {
        return yiYiWorldBookDao.getAllBooks()
    }

    suspend fun getBookWithEntriesById(id: String): YiYiWorldBookWithEntries? {
        return yiYiWorldBookDao.getBookWithEntriesById(id)
    }

    fun getAllBooksWithEntriesFlow(): Flow<List<YiYiWorldBookWithEntries>> {
        return yiYiWorldBookDao.getAllBooksWithEntriesFlow()
    }
}
