package com.wenchen.yiyi.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "yiyi_regex_scripts")
data class YiYiRegexScript(
    /** 脚本唯一标识符 */
    @PrimaryKey val id: String,
    /** 所属的分组 ID */
    val groupId : String,
    /** 脚本名称 */
    val scriptName: String? = null,
    /** 要查找匹配的正则表达式 */
    val findRegex: String? = null,
    /** 要替换成的字符串 */
    val replaceString: String? = null,
    /** 替换后需要修剪掉的特定字符串 */
    val trimStrings: List<String>? = emptyList(),
    /** 脚本执行的位置（如：0 为用户输入前，1 为 AI 回复后） */
    val placement: List<Int>? = emptyList(),
    /** 是否禁用此脚本 */
    val disabled: Boolean? = false,
    /** 是否仅作用于 Markdown 文本 */
    val markdownOnly: Boolean? = false,
    /** 是否仅作用于发送给 AI 的提示词（Prompt） */
    val promptOnly: Boolean? = false,
    /** 编辑消息时是否重新运行脚本 */
    val runOnEdit: Boolean? = false,
    /** 是否执行正则表达式的变量替换 */
    val substituteRegex: Int? = null,
    /** 作用的消息历史最小深度 */
    val minDepth: Int? = null,
    /** 作用的消息历史最大深度 */
    val maxDepth: Int? = null
)

@Serializable
@Entity(tableName = "yiyi_regex_groups")
data class YiYiRegexGroup(
    /** 分组唯一标识符 */
    @PrimaryKey val id: String,
    /** 分组名称 */
    val name: String? = null,
    /** 分组描述 */
    val description: String? = null,
    /** 资源所有者列表 */
    val source_owner: List<String>? = emptyList(),
    /** 创建时间 */
    val createTime: Long? = null,
    /** 更新时间 */
    val updateTime: Long? = null
)
