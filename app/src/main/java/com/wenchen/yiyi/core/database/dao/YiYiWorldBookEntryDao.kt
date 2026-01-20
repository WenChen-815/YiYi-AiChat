package com.wenchen.yiyi.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface YiYiWorldBookEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: YiYiWorldBookEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<YiYiWorldBookEntry>)

    @Update
    suspend fun updateEntry(entry: YiYiWorldBookEntry): Int

    @Delete
    suspend fun deleteEntry(entry: YiYiWorldBookEntry): Int

    @Query("SELECT * FROM yiyi_world_book_entries WHERE entryId = :id")
    suspend fun getEntryById(id: String): YiYiWorldBookEntry?

    @Query("SELECT * FROM yiyi_world_book_entries WHERE bookId = :bookId ORDER BY displayIndex ASC")
    fun getEntriesByBookIdFlow(bookId: String): Flow<List<YiYiWorldBookEntry>>

    @Query("SELECT * FROM yiyi_world_book_entries WHERE bookId = :bookId ORDER BY displayIndex ASC")
    suspend fun getEntriesByBookId(bookId: String): List<YiYiWorldBookEntry>

    @Query("DELETE FROM yiyi_world_book_entries WHERE bookId = :bookId")
    suspend fun deleteEntriesByBookId(bookId: String): Int
}
