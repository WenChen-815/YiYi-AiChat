package com.wenchen.yiyi.common.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 1,
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}