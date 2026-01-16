package com.wenchen.yiyi.core.database.di

import com.wenchen.yiyi.core.database.AppDatabase
import android.content.Context
import androidx.room.Room
import com.wenchen.yiyi.core.database.dao.AICharacterDao
import com.wenchen.yiyi.core.database.dao.AIChatMemoryDao
import com.wenchen.yiyi.core.database.dao.ChatMessageDao
import com.wenchen.yiyi.core.database.dao.ConversationDao
import com.wenchen.yiyi.core.database.dao.TempChatMessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库模块
 * 负责提供数据库实例及相关DAO的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供数据库实例
     *
     * @param context 应用上下文
     * @return 应用数据库实例
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4)
            .build()
    }

    /**
     * 提供角色DAO实例
     *
     * @param database 应用数据库实例
     * @return 角色DAO实例
     */
    @Provides
    @Singleton
    fun provideCharacterDao(
        database: AppDatabase
    ): AICharacterDao {
        return database.aiCharacterDao()
    }

    /**
     * 提供AI记忆DAO实例
     *
     * @param database 应用数据库实例
     * @return AI记忆DAO实例
     */
    @Provides
    @Singleton
    fun provideChatMemoryDao(
        database: AppDatabase
    ): AIChatMemoryDao {
        return database.aiChatMemoryDao()
    }

    /**
     * 提供聊天记录DAO实例
     *
     * @param database 应用数据库实例
     * @return 聊天记录DAO实例
     */
    @Provides
    @Singleton
    fun provideChatMessageDao(
        database: AppDatabase
    ): ChatMessageDao {
        return database.chatMessageDao()
    }

    /**
     * 提供临时聊天记录DAO实例
     *
     * @param database 应用数据库实例
     * @return 临时聊天记录DAO实例
     */
    @Provides
    @Singleton
    fun provideTempChatMessageDao(
        database: AppDatabase
    ): TempChatMessageDao {
        return database.tempChatMessageDao()
    }

    /**
     * 提供对话DAO实例
     *
     * @param database 应用数据库实例
     * @return 对话DAO实例
     */
    @Provides
    @Singleton
    fun provideConversationDao(
        database: AppDatabase
    ): ConversationDao {
        return database.conversationDao()
    }
}
