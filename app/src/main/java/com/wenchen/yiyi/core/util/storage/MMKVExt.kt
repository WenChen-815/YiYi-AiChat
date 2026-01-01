package com.wenchen.yiyi.core.util.storage

import com.tencent.mmkv.MMKV
import kotlinx.serialization.json.Json

inline fun <reified T> MMKV.putObject(key: String, value: T) {
    val json = Json.encodeToString(value)
    putString(key, json)
}

inline fun <reified T> MMKV.getObject(key: String): T? {
    val json = getString(key, "") ?: ""
    return if (json.isNotEmpty()) {
        try {
            Json.decodeFromString<T>(json)
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }
}