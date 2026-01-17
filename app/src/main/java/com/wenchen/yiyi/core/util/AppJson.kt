package com.wenchen.yiyi.core.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * 通用的 Json 配置，用于序列化和反序列化
 */
val AppJson = Json {
    ignoreUnknownKeys = true // 忽略 JSON 中多余的字段（向上兼容）
    coerceInputValues = true // 如果类型不匹配（如 null 赋给了非空字段），尝试使用默认值（向下兼容）
    encodeDefaults = true    // 序列化时包含默认值
}

@OptIn(ExperimentalSerializationApi::class)
val PrettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "    "
    ignoreUnknownKeys = true
}