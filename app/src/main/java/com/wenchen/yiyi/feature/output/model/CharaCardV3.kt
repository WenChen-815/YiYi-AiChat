package com.wenchen.yiyi.feature.output.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * SillyTavern 角色卡 V3 协议数据类
 * 参考规范版本：3.0
 */

@Serializable
data class CharaCardV3(
    /** 协议标识，必须为 "chara_card_v3" */
    val spec: String = "chara_card_v3",
    /** 协议版本，必须为 "3.0" */
    val spec_version: String = "3.0",
    /** 核心数据容器 */
    val data: CharacterData
)

@Serializable
data class CharacterData(
    // --- V1 & V2 核心字段 ---
    val name: String,
    val description: String,
    val personality: String? = null,
    val scenario: String? = null,
    val first_mes: String? = null,
    val mes_example: String? = null,

    // --- 扩展与元数据 ---
    val avatar: String? = null,
    val creator_notes: String = "",
    val system_prompt: String = "",
    val post_history_instructions: String = "",
    val alternate_greetings: List<String>? = emptyList(),
    val character_book: CharacterBook? = null,
    val tags: List<String> = emptyList(),
    val creator: String = "",
    val character_version: String = "",
    val nickname: String? = null,
    val creator_notes_multilingual: JsonObject? = null,
    val source: String? = null,
    val group_only_greetings: List<String>? = emptyList(),
    val creation_date: Long? = null,
    val modification_date: Long? = null,

    /** 核心扩展数据，包含脚本和源信息 */
    val extensions: JsonObject? = null
)

@Serializable
data class CharacterExtensions(
    val source_owner: List<String>? = emptyList(),
    val regex_scripts: List<RegexScript>? = emptyList()
)

@Serializable
data class RegexScript(
    val id: String? = null,
    val scriptName: String? = null,
    val findRegex: String? = null,
    val replaceString: String? = null,
    val trimStrings: List<String>? = emptyList(),
    val placement: List<Int>? = emptyList(),
    val disabled: Boolean? = false,
    val markdownOnly: Boolean? = false,
    val promptOnly: Boolean? = false,
    val runOnEdit: Boolean? = false,
    val substituteRegex: Int? = null,
    val minDepth: Int? = null,
    val maxDepth: Int? = null
)

/**
 * 角色专属世界书（Character Book）定义
 */
@Serializable
data class CharacterBook(
    val name: String? = null,
    val description: String? = null,
    val scan_depth: Int? = null,
    val token_budget: Int? = null,
    val recursive_scanning: Boolean? = null,
    val entries: List<CharacterBookEntry> = emptyList(),
    val extensions: JsonObject? = null
)

/**
 * 世界书条目定义
 */
@Serializable
data class CharacterBookEntry(
    val keys: List<String>,
    val content: String,
    val enabled: Boolean,
    val insertion_order: Int,
    val case_sensitive: Boolean? = null,
    val name: String? = null,
    val priority: Int? = null,
    val comment: String? = null,
    val constant: Boolean? = null,
    val position: String? = null, // "before_char" | "after_char"
    val use_regex: Boolean? = null,
    val id: Int? = null,
    val display_index: Int? = null,
    val selective: Boolean? = null,
    val extensions: CharacterBookEntryExtensions? = null
)

/**
 * 世界书条目的高级扩展设置
 */
@Serializable
data class CharacterBookEntryExtensions(
    val selectiveLogic: Int? = null,
    val position: Int? = null,
    val depth: Int? = null,
    val role: Int? = null,
    val match_whole_words: Boolean? = null,
    val probability: Int? = null,
    val useProbability: Boolean? = null,
    val sticky: Int? = null,
    val cooldown: Int? = null,
    val delay: Int? = null,
    val exclude_recursion: Boolean? = null,
    val prevent_recursion: Boolean? = null,
    val delay_until_recursion: Boolean? = null,
    val group: String? = null,
    val group_override: Boolean? = null,
    val group_weight: Int? = null,
    val use_group_scoring: Boolean? = null,
    val scan_depth: Int? = null,
    val case_sensitive: Boolean? = null,
    val automation_id: String? = null,
    val vectorized: Boolean? = null
)
