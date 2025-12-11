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
        // 解析AI回复中的日期和角色名称
        val dateRegex = Regex("\\d{4}-\\d{2}-\\d{2} \\w+ \\d{2}:\\d{2}:\\d{2}\\|")
        val roleRegex = Regex("^\\[[^]]*]")

        // 分别处理日期和角色名称的移除
        var result = content
        result = dateRegex.replace(result, "")
        result = roleRegex.replace(result, "")

        return result.trim()
    }
}