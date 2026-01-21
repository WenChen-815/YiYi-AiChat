package com.wenchen.yiyi.core.database.entity

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "yiyi_world_books")
data class YiYiWorldBook(
    /** 世界书籍唯一标识符 */
    @PrimaryKey val id: String,
    /** 书籍名称 */
    val name: String? = null,
    /** 书籍描述 */
    val description: String? = null,
    /** 创建时间 */
    val createTime: Long? = null,
    /** 更新时间 */
    val updateTime: Long? = null
)

@Serializable
@Entity(
    tableName = "yiyi_world_book_entries",
    foreignKeys = [
        ForeignKey(
            entity = YiYiWorldBook::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE // 书籍删除时，条目自动删除
        )
    ],
    indices = [Index(value = ["bookId"])]
)

@Keep
data class YiYiWorldBookEntry(
    @PrimaryKey val entryId: String,
    val bookId: String, // 关联 YiYiWorldBook 的 id
    val keys: List<String> = emptyList(), // 触发关键词，需配套 TypeConverter
    val content: String? = null,
    val enabled: Boolean = true,
    val useRegex: Boolean = false,
    val insertionOrder: Int = 0,
    val name: String? = null,
    val comment: String? = null,
    val selective: Boolean = false,
    val caseSensitive: Boolean = false,
    val constant: Boolean = true,
    val position: String = "before_char",
    val displayIndex: Int = 0,

    // --- Extensions 扁平化 ---
    @Embedded(prefix = "ext_") // 给扩展字段增加前缀，防止重名冲突
    val extensions: WorldBookEntryExtensions = WorldBookEntryExtensions()
)

@Keep
@Serializable
data class WorldBookEntryExtensions(
    val selectiveLogic: Int = 0,
    val position: Int = 0,
    val depth: Int = 4,
    val role: Int = 0,
    val matchWholeWords: Boolean = true,
    val probability: Int = 100,
    val useProbability: Boolean = true,
    val sticky: Int = 0,
    val cooldown: Int = 0,
    val delay: Int = 0,
    val excludeRecursion: Boolean = false,
    val preventRecursion: Boolean = false,
    val delayUntilRecursion: Boolean = false,
    val group: String = "",
    val groupOverride: Boolean = false,
    val groupWeight: Int = 100,
    val useGroupScoring: Boolean = false,
    val scanDepth: Int? = null,
    val caseSensitive: Boolean = false,
    val automationId: String = "",
    val vectorized: Boolean = false
)

data class YiYiWorldBookWithEntries(
    @Embedded val book: YiYiWorldBook,
    @Relation(
        parentColumn = "id",
        entityColumn = "bookId"
    )
    val entries: List<YiYiWorldBookEntry>
)