package com.wenchen.yiyi.feature.output.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * SillyTavern 角色卡 V3 协议数据类
 * 参考规范版本：3.0
 */

@Serializable
data class CharaCardV3(
    /** 协议标识，固定为 "chara_card_v3" */
    val spec: String = "chara_card_v3",
    /** 协议版本，目前为 "3.0" */
    val spec_version: String = "3.0",
    /** 核心数据容器，包含角色的所有具体设定 */
    val data: CharacterData
)

@Serializable
data class CharacterData(
    // --- V1 & V2 核心字段 ---
    /** 角色的名字 */
    val name: String,
    /** 角色的主要描述（通常包含外貌、身世、性格背景等核心信息） */
    val description: String,
    /** 角色的性格特征（简短的标签或性格描述） */
    val personality: String? = null,
    /** 当前对话的场景设定（如：在一个下雨的咖啡馆里） */
    val scenario: String? = null,
    /** 角色发出的第一条消息（开场白） */
    val first_mes: String? = null,
    /** 对话示例（用于向 AI 演示该角色说话的语气、风格和格式） */
    val mes_example: String? = null,

    // --- 扩展与元数据 ---
    /** 角色头像的路径或标识符 */
    val avatar: String? = null,
    /** 作者备注（创作者留下的说明或提示词使用建议） */
    val creator_notes: String = "",
    /** 系统提示词（直接发送给 AI 的底层指令，用于塑造行为准则） */
    val system_prompt: String = "",
    /** 后置历史指令（在所有聊天记录之后插入的指令，用于引导 AI 下一句话的方向） */
    val post_history_instructions: String = "",
    /** 备选开场白列表（用户可以从多个开场白中切换） */
    val alternate_greetings: List<String>? = emptyList(),
    /** 关联的角色专属世界书（Lorebook），用于存储特定名词的背景知识 */
    val character_book: CharacterBook? = null,
    /** 角色的分类标签（如：奇幻、现代、傲娇等） */
    val tags: List<String> = emptyList(),
    /** 角色卡的原创作者名 */
    val creator: String = "",
    /** 该角色卡自身的版本号 */
    val character_version: String = "",
    /** 角色的昵称 */
    val nickname: String? = null,
    /** 多语言版本的作者备注 */
    val creator_notes_multilingual: JsonObject? = null,
    /** 角色来源（例如原著名称或下载链接） */
    val source: String? = null,
    /** 仅在群聊中使用的特殊开场白 */
    val group_only_greetings: List<String>? = emptyList(),
    /** 角色卡创建时间（毫秒时间戳） */
    val creation_date: Long? = null,
    /** 角色卡最后修改时间（毫秒时间戳） */
    val modification_date: Long? = null,
    /** 核心扩展数据容器，包含脚本和第三方扩展信息 */
    val extensions: JsonObject? = null
)

@Serializable
data class CharacterExtensions(
    /** 资源所有者列表 */
    val source_owner: List<String>? = emptyList(),
    /** 角色专用的正则处理脚本（用于自动替换 AI 回复中的特定内容） */
    val regex_scripts: List<RegexScript>? = emptyList()
)

@Serializable
data class RegexScript(
    /** 脚本唯一标识符 */
    val id: String? = null,
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

/**
 * 角色专属世界书（Character Book / Lorebook）定义
 * 用于当对话提到特定词汇时，动态给 AI 补充背景知识
 */
@Serializable
data class CharacterBook(
    /** 世界书名称 */
    val name: String? = null,
    /** 世界书描述 */
    val description: String? = null,
    /** 扫描对话历史的条数（决定往前看多少句来触发关键词） */
    val scan_depth: Int? = null,
    /** 该世界书允许占用的最大 Token 数量 */
    val token_budget: Int? = null,
    /** 是否开启递归扫描（条目触发的内容是否可以触发其他条目） */
    val recursive_scanning: Boolean? = null,
    /** 世界书中的具体知识条目列表 */
    val entries: List<CharacterBookEntry> = emptyList(),
    /** 扩展数据 */
    val extensions: JsonObject? = null
)

/**
 * 世界书条目定义（例如：“龙”这个词对应的详细背景介绍）
 */
@Serializable
data class CharacterBookEntry(
    /** 触发该条目的关键词列表 */
    val keys: List<String>,
    /** 触发后要插入到提示词中的具体背景内容 */
    val content: String,
    /** 该条目是否处于启用状态 */
    val enabled: Boolean,
    /** 插入顺序（数字越小，在发送给 AI 的文本中越靠前） */
    val insertion_order: Int,
    /** 匹配关键词时是否区分大小写 */
    val case_sensitive: Boolean? = null,
    /** 条目备注名称 */
    val name: String? = null,
    /** 优先级（当 Token 超出预算时，决定优先保留哪些条目） */
    val priority: Int? = null,
    /** 创作者备注 */
    val comment: String? = null,
    /** 是否常驻（如果为 true，则不看关键词，始终插入该内容） */
    val constant: Boolean? = null,
    /** 插入位置（"before_char": 角色设定前, "after_char": 角色设定后） */
    val position: String? = null,
    /** 关键词是否作为正则表达式处理 */
    val use_regex: Boolean? = null,
    /** 条目唯一 ID */
    val id: Int? = null,
    /** 在 UI 列表中的显示顺序 */
    val display_index: Int? = null,
    /** 是否开启选择性逻辑（需配合多个关键词使用） */
    val selective: Boolean? = null,
    /** 条目的高级扩展设置 */
    val extensions: CharacterBookEntryExtensions? = null
)

/**
 * 世界书条目的高级扩展设置
 */
@Serializable
data class CharacterBookEntryExtensions(
    /** 选择性匹配逻辑（0: 逻辑与, 1: 逻辑非） */
    val selectiveLogic: Int? = null,
    /** 具体的插入位置偏移 */
    val position: Int? = null,
    /** 插入深度 */
    val depth: Int? = null,
    /** 关联的消息角色（0: 系统, 1: 用户, 2: 助手） */
    val role: Int? = null,
    /** 是否仅全字匹配关键词 */
    val match_whole_words: Boolean? = null,
    /** 触发概率 (0-100) */
    val probability: Int? = null,
    /** 是否使用概率触发 */
    val useProbability: Boolean? = null,
    /** 触发后内容持续生效的消息条数（粘性） */
    val sticky: Int? = null,
    /** 触发后的冷却消息条数（冷却期间不重复触发） */
    val cooldown: Int? = null,
    /** 触发后延迟多少条消息再插入内容 */
    val delay: Int? = null,
    /** 是否排除在递归扫描之外 */
    val exclude_recursion: Boolean? = null,
    /** 是否防止该条目触发后续的递归扫描 */
    val prevent_recursion: Boolean? = null,
    /** 是否延迟到递归扫描阶段处理 */
    val delay_until_recursion: Boolean? = null,
    /** 逻辑分组名称 */
    val group: String? = null,
    /** 是否覆盖所在组的设置 */
    val group_override: Boolean? = null,
    /** 分组权重 */
    val group_weight: Int? = null,
    /** 是否使用分组评分机制 */
    val use_group_scoring: Boolean? = null,
    /** 单独覆盖该条目的扫描深度 */
    val scan_depth: Int? = null,
    /** 单独覆盖该条目的大小写敏感度 */
    val case_sensitive: Boolean? = null,
    /** 自动化处理用的标识符 */
    val automation_id: String? = null,
    /** 是否使用向量搜索（RAG）来触发（而非传统的关键词匹配） */
    val vectorized: Boolean? = null
)