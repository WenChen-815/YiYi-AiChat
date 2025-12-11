package com.wenchen.yiyi.config.common

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.wenchen.yiyi.common.App

class ConfigManager() {
    private val PREF_NAME = "ai_chat_config"
    private val prefs: SharedPreferences =
        App.instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // 检查是否有完整的配置
    fun hasCompleteConfig(): Boolean {
        return !getApiKey().isNullOrEmpty() &&
                !getBaseUrl().isNullOrEmpty() &&
                !getSelectedModel().isNullOrEmpty() &&
                !getUserName().isNullOrEmpty() &&
                !getUserId().isNullOrEmpty()
    }


    // ======基础API配置=============================================================================
    private val KEY_API_KEY = "api_key"
    fun saveApiKey(apiKey: String) {
        prefs.edit { putString(KEY_API_KEY, apiKey) }
    }

    fun getApiKey(): String? {
        return prefs.getString(KEY_API_KEY, null)
    }

    private val KEY_BASE_URL = "base_url"

    fun saveBaseUrl(baseUrl: String) {
        prefs.edit { putString(KEY_BASE_URL, baseUrl) }
    }

    fun getBaseUrl(): String? {
        return prefs.getString(KEY_BASE_URL, null)
    }

    private val KEY_SELECTED_MODEL = "selected_model"

    fun saveSelectedModel(modelId: String) {
        prefs.edit { putString(KEY_SELECTED_MODEL, modelId) }
    }

    fun getSelectedModel(): String? {
        return prefs.getString(KEY_SELECTED_MODEL, null)
    }
    // =============================================================================================


    // ======用户相关配置=============================================================================
    private val USER_NAME = "user_name"

    fun saveUserName(userName: String) {
        prefs.edit { putString(USER_NAME, userName) }
    }

    fun getUserName(): String? {
        return prefs.getString(USER_NAME, null)
    }

    private val USER_ID = "user_id"

    fun saveUserId(userId: String) {
        prefs.edit { putString(USER_ID, userId) }
    }

    fun getUserId(): String? {
        return prefs.getString(USER_ID, null)
    }

    private val KEY_USER_AVATAR_PATH = "user_avatar_path"

    // 保存用户头像路径
    fun saveUserAvatarPath(avatarPath: String?) {
        prefs.edit {
            if (avatarPath != null) {
                putString(KEY_USER_AVATAR_PATH, avatarPath)
            } else {
                remove(KEY_USER_AVATAR_PATH)
            }
        }
    }

    // 获取用户头像路径
    fun getUserAvatarPath(): String? {
        return prefs.getString(KEY_USER_AVATAR_PATH, null)
    }
    // =============================================================================================


    // 选中的角色相关
    fun saveSelectedCharacterId(characterId: String) {
        prefs.edit { putString("selected_character_id", characterId) }
    }

    fun getSelectedCharacterId(): String? {
        return prefs.getString("selected_character_id", null)
    }

    // 清除选中的角色
    fun clearSelectedCharacter() {
        prefs.edit { remove("selected_character_id") }
    }

    // 清除所有配置
    fun clearAllConfig() {
        prefs.edit { clear() }
    }

    // ======图片识别相关配置==========================================================================
    private val KEY_IMG_RECOGNITION_ENABLED = "img_recognition_enabled"

    // 保存图片识别开关状态
    fun saveImgRecognitionEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_IMG_RECOGNITION_ENABLED, enabled) }
    }

    // 获取图片识别开关状态
    fun isImgRecognitionEnabled(): Boolean {
        return prefs.getBoolean(KEY_IMG_RECOGNITION_ENABLED, false)
    }

    private val KEY_IMG_API_KEY = "img_api_key"

    // 保存图片识别API Key
    fun saveImgApiKey(apiKey: String) {
        prefs.edit { putString(KEY_IMG_API_KEY, apiKey) }
    }

    // 获取图片识别API Key
    fun getImgApiKey(): String? {
        return prefs.getString(KEY_IMG_API_KEY, null)
    }

    private val KEY_IMG_BASE_URL = "img_base_url"

    // 保存图片识别中转地址
    fun saveImgBaseUrl(baseUrl: String) {
        prefs.edit { putString(KEY_IMG_BASE_URL, baseUrl) }
    }

    // 获取图片识别中转地址
    fun getImgBaseUrl(): String? {
        return prefs.getString(KEY_IMG_BASE_URL, null)
    }

    private val KEY_SELECTED_IMG_MODEL = "selected_img_model"

    // 保存选中的图片识别模型
    fun saveSelectedImgModel(modelId: String) {
        prefs.edit { putString(KEY_SELECTED_IMG_MODEL, modelId) }
    }

    // 获取选中的图片识别模型
    fun getSelectedImgModel(): String? {
        return prefs.getString(KEY_SELECTED_IMG_MODEL, null)
    }
    // =============================================================================================


    // ======其他对话设置========================================================================
    private val KEY_MAX_CONTEXT_MESSAGE_SIZE = "max_context_message_size"

    // 保存最大上下文消息数
    fun saveMaxContextMessageSize(size: Int) {
        prefs.edit { putInt(KEY_MAX_CONTEXT_MESSAGE_SIZE, size) }
    }

    // 获取最大上下文消息数
    fun getMaxContextMessageSize(): Int {
        return prefs.getInt(KEY_MAX_CONTEXT_MESSAGE_SIZE, 5)
    }

    private val KEY_SUMMARIZE_TRIGGER_COUNT = "summarize_trigger_count"

    // 保存触发总结的对话数
    fun saveSummarizeTriggerCount(count: Int) {
        prefs.edit { putInt(KEY_SUMMARIZE_TRIGGER_COUNT, count) }
    }

    // 获取触发总结的对话数
    fun getSummarizeTriggerCount(): Int {
        return prefs.getInt(KEY_SUMMARIZE_TRIGGER_COUNT, 20)
    }

    private val KEY_MAX_SUMMARIZE_COUNT = "max_summarize_count"

    // 保存最大总结次数
    fun saveMaxSummarizeCount(count: Int) {
        prefs.edit { putInt(KEY_MAX_SUMMARIZE_COUNT, count) }
    }

    // 获取最大总结次数
    fun getMaxSummarizeCount(): Int {
        return prefs.getInt(KEY_MAX_SUMMARIZE_COUNT, 20)
    }

    private val KEY_ENABLE_SEPARATOR = "enable_separator"

    // 保存分隔符启用状态
    fun saveEnableSeparator(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLE_SEPARATOR, enabled) }
    }

    // 获取分隔符启用状态
    fun isSeparatorEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_SEPARATOR, false)
    }

    private val KEY_ENABLE_TIME_PREFIX = "enable_time_prefix"

    // 保存时间前缀启用状态
    fun saveEnableTimePrefix(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLE_TIME_PREFIX, enabled) }
    }

    // 获取时间前缀启用状态
    fun isTimePrefixEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_TIME_PREFIX, true)
    }
    // =============================================================================================
}