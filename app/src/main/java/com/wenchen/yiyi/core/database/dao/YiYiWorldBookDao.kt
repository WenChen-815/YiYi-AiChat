package com.wenchen.yiyi.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.wenchen.yiyi.core.database.entity.YiYiWorldBook
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookWithEntries
import kotlinx.coroutines.flow.Flow

@Dao
interface YiYiWorldBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: YiYiWorldBook): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<YiYiWorldBook>)

    @Update
    suspend fun updateBook(book: YiYiWorldBook): Int

    @Delete
    suspend fun deleteBook(book: YiYiWorldBook): Int

    @Query("SELECT * FROM yiyi_world_books WHERE id = :id")
    suspend fun getBookById(id: String): YiYiWorldBook?

    @Query("SELECT * FROM yiyi_world_books ORDER BY updateTime DESC")
    fun getAllBooksFlow(): Flow<List<YiYiWorldBook>>

    @Query("SELECT * FROM yiyi_world_books ORDER BY updateTime DESC")
    suspend fun getAllBooks(): List<YiYiWorldBook>

    @Transaction
    @Query("SELECT * FROM yiyi_world_books WHERE id = :id")
    suspend fun getBookWithEntriesById(id: String): YiYiWorldBookWithEntries?

    @Transaction
    @Query("SELECT * FROM yiyi_world_books")
    fun getAllBooksWithEntriesFlow(): Flow<List<YiYiWorldBookWithEntries>>
}
