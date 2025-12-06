package com.wenchen.yiyi.common.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wenchen.yiyi.aiChat.entity.ChatMessage
import com.wenchen.yiyi.aiChat.entity.Conversation
import com.wenchen.yiyi.aiChat.entity.TempChatMessage
import com.wenchen.yiyi.aiChat.entity.dao.ChatMessageDao
import com.wenchen.yiyi.aiChat.entity.dao.ConversationDao
import com.wenchen.yiyi.aiChat.entity.dao.TempChatMessageDao
import com.wenchen.yiyi.common.entity.dao.AICharacterDao
import com.wenchen.yiyi.aiChat.entity.dao.AIChatMemoryDao
import com.wenchen.yiyi.common.entity.AICharacter
import com.wenchen.yiyi.aiChat.entity.AIChatMemory


@Database(
    entities = [
        AICharacter::class,
        ChatMessage::class,
        TempChatMessage::class,
        AIChatMemory::class,
        Conversation::class,
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aiCharacterDao(): AICharacterDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun aiChatMemoryDao(): AIChatMemoryDao
    abstract fun tempChatMessageDao(): TempChatMessageDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        // 单例模式防止多次实例化数据库
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_chat_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 conversations 表添加 characterKeywords 列
                db.execSQL(
                    "ALTER TABLE conversations ADD COLUMN characterKeywords TEXT"
                )
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 conversations 表添加 additionalSummaryRequirement 列
                db.execSQL(
                    "ALTER TABLE conversations ADD COLUMN additionalSummaryRequirement TEXT"
                )
            }
        }
    }
}