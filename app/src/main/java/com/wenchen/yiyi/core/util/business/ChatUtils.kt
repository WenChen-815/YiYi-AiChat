package com.wenchen.yiyi.core.util.business

import com.wenchen.yiyi.core.database.entity.ChatMessage
import com.wenchen.yiyi.core.database.entity.TempChatMessage

object ChatUtils {
    data class ParsedMessage(
        val cleanedContent: String,
        val extractedDate: String? = null,
        val extractedRole: String? = null
    )
    fun parseMessage(message: ChatMessage): ParsedMessage {
        return parse(message.content)
    }

    fun parseMessage(message: TempChatMessage): ParsedMessage {
        return parse(message.content)
    }

    fun parse(content: String): ParsedMessage {
        // 解析AI回复中的日期和角色名称
        val dateRegex = Regex("\\d{4}-\\d{2}-\\d{2} \\w+ \\d{2}:\\d{2}:\\d{2}\\|")
        val roleRegex = Regex("^\\[[^]]*]")

        var cleanedContent = content
        var extractedDate: String? = null
        var extractedRole: String? = null

        // 提取并移除日期
        val dateMatch = dateRegex.find(cleanedContent)
        if (dateMatch != null) {
            extractedDate = dateMatch.value
            cleanedContent = dateRegex.replace(cleanedContent, "")
        }

        // 提取并移除角色名称
        val roleMatch = roleRegex.find(cleanedContent)
        if (roleMatch != null) {
            extractedRole = roleMatch.value
            cleanedContent = roleRegex.replace(cleanedContent, "")
        }

        return ParsedMessage(
            cleanedContent = cleanedContent.trim(),
            extractedDate = extractedDate,
            extractedRole = extractedRole
        )
    }
}