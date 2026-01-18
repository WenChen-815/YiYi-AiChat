package com.wenchen.yiyi.core.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.core.database.entity.MessageContentType
import com.wenchen.yiyi.core.database.entity.MessageType

class Converters {

    @TypeConverter
    fun fromMessageType(type: MessageType): String {
        return type.name
    }

    @TypeConverter
    fun toMessageType(type: String): MessageType {
        return MessageType.valueOf(type)
    }

    @TypeConverter
    fun fromMessageContentType(type: MessageContentType): String {
        return type.name
    }

    @TypeConverter
    fun toMessageContentType(type: String): MessageContentType {
        return MessageContentType.valueOf(type)
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Long {
        return value ?: System.currentTimeMillis()
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(it, listType)
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return value?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        return value?.let {
            val listType = object : TypeToken<List<Int>>() {}.type
            Gson().fromJson(it, listType)
        }
    }

    @TypeConverter
    fun fromConversationType(type: ConversationType): String {
        return type.name
    }

    @TypeConverter
    fun toConversationType(type: String): ConversationType {
        return ConversationType.valueOf(type)
    }

    @TypeConverter
    fun fromCharacterIdMap(map: Map<String, Float>?): String? {
        return map?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toCharacterIdMap(value: String?): Map<String, Float>? {
        return value?.let {
            val mapType = object : TypeToken<Map<String, Float>>() {}.type
            Gson().fromJson(it, mapType)
        }
    }

    @TypeConverter
    fun fromCharacterKeywordsMap(map: Map<String, List<String>>?): String? {
        return map?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toCharacterKeywordsMap(value: String?): Map<String, List<String>>? {
        return value?.let {
            val mapType = object : TypeToken<Map<String, List<String>>>() {}.type
            Gson().fromJson(it, mapType)
        }
    }
}