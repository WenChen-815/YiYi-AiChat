package com.wenchen.yiyi.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wenchen.yiyi.core.database.entity.ChatMessage
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.TempChatMessage
import com.wenchen.yiyi.core.database.dao.ChatMessageDao
import com.wenchen.yiyi.core.database.dao.ConversationDao
import com.wenchen.yiyi.core.database.dao.TempChatMessageDao
import com.wenchen.yiyi.core.database.dao.AICharacterDao
import com.wenchen.yiyi.core.database.dao.AIChatMemoryDao
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.core.database.entity.AIChatMemory

/**
 * 应用数据库
 */
@Database(
    entities = [
        AICharacter::class,
        ChatMessage::class,
        TempChatMessage::class,
        AIChatMemory::class,
        Conversation::class,
    ],
    version = 4,
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
        const val DATABASE_NAME = "ai_chat_database"

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 创建新结构的临时表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ai_characters_new` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `userId` TEXT NOT NULL, 
                        `description` TEXT NOT NULL, 
                        `first_mes` TEXT, 
                        `mes_example` TEXT, 
                        `personality` TEXT, 
                        `scenario` TEXT, 
                        `creator_notes` TEXT, 
                        `system_prompt` TEXT, 
                        `post_history_instructions` TEXT, 
                        `avatar` TEXT, 
                        `background` TEXT, 
                        `creation_date` INTEGER NOT NULL, 
                        `modification_date` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 2. 迁移数据并合并字段
                // 将旧表的 aiCharacterId 映射到新表的 id
                db.execSQL("""
                    INSERT INTO ai_characters_new (
                        id, name, userId, description, mes_example, 
                        avatar, background, creation_date, modification_date
                    )
                    SELECT 
                        aiCharacterId, name, userId, 
                        ('# 角色身份' || CHAR(10) || COALESCE(roleIdentity, '') || CHAR(10) || CHAR(10) || 
                         '# 角色外貌' || CHAR(10) || COALESCE(roleAppearance, '') || CHAR(10) || CHAR(10) || 
                         '# 角色描述' || CHAR(10) || COALESCE(roleDescription, '') || CHAR(10) || CHAR(10) || 
                         '# 行为规则' || CHAR(10) || COALESCE(behaviorRules, '')), 
                        outputExample, avatarPath, backgroundPath, createdAt, createdAt
                    FROM ai_characters
                """.trimIndent())

                // 3. 删除旧表
                db.execSQL("DROP TABLE ai_characters")

                // 4. 重命名新表
                db.execSQL("ALTER TABLE ai_characters_new RENAME TO ai_characters")
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