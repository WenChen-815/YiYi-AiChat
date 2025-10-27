package com.wenchen.yiyi.common.utils

import com.wenchen.yiyi.aiChat.entity.ChatMessage
import com.wenchen.yiyi.aiChat.entity.TempChatMessage

object ChatUtil {
    fun parseMessage(message: ChatMessage): String {
        return parse(message.content)
    }

    fun parseMessage(message: TempChatMessage): String {
        return parse(message.content)
    }

    fun parse(content: String): String {
        var cleanedContent = content
        // 解析AI回复中的日期和角色名称
        val dateRegex = Regex("\\d{4}-\\d{2}-\\d{2} \\w+ \\d{2}:\\d{2}:\\d{2}")
        val roleRegex = Regex("\\[.*?\\]")

        val dateMatch = dateRegex.find(content)
        val roleMatch = roleRegex.find(content)
        if (dateMatch != null && roleMatch != null) {
            // 提取日期和角色名称
            val date = dateMatch.value
            val role = roleMatch.value

            // 移除日期和角色名称，只保留实际回复内容
            cleanedContent = content.replace("${date}|$role", "").trim()
        }
        return cleanedContent
    }
}