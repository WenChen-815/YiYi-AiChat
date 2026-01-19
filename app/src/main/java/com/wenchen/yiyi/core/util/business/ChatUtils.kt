package com.wenchen.yiyi.core.util.business

import com.wenchen.yiyi.core.database.entity.ChatMessage
import com.wenchen.yiyi.core.database.entity.TempChatMessage
import com.wenchen.yiyi.core.database.entity.YiYiRegexScript
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class ChatUtils {
    fun regexReplace(content: String, regexScripts: List<YiYiRegexScript>): String {
        if (content.isEmpty() || regexScripts.isEmpty()) return content

        var result = content
        regexScripts.forEach { script ->
            // 检查是否禁用
            if (script.disabled == true) return@forEach

            val pattern = script.findRegex
            // 检查正则是否为空
            if (pattern.isNullOrEmpty()) return@forEach

            try {
                // 捕获正则语法异常，防止非法输入导致崩溃
                val regex = pattern.toRegex()
                val replacement = script.replaceString ?: ""

                result = result.replace(regex, replacement)

                // 处理后期修剪逻辑 (如需要)
                script.trimStrings?.forEach { trimStr ->
                    if (trimStr.isNotEmpty()) {
                        result = result.replace(trimStr, "")
                    }
                }
            } catch (e: Exception) {
                // 记录日志或静默跳过错误的正则
                Timber.e("Regex error in script ${script.id}: ${e.message}")
            }
        }
        return result
    }
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