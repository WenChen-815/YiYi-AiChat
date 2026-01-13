package com.wenchen.yiyi.core.util.storage

import com.tencent.mmkv.MMKV
import com.wenchen.yiyi.core.util.AppJson
import timber.log.Timber

inline fun <reified T> MMKV.putObject(key: String, value: T) {
    val json = AppJson.encodeToString(value)
    putString(key, json)
}

inline fun <reified T> MMKV.getObject(key: String): T? {
    val json = getString(key, "") ?: ""
    return if (json.isNotEmpty()) {
        try {
            AppJson.decodeFromString<T>(json)
        } catch (e: Exception) {
            Timber.e(e, "Error decoding JSON for key $key")
            null
        }
    } else {
        Timber.d("No object found for key $key")
        null
    }
}