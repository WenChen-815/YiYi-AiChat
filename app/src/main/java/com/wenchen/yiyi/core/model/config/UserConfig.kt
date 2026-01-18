package com.wenchen.yiyi.core.model.config

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import kotlinx.serialization.Serializable

@Serializable
data class UserConfig(
    // ======用户相关配置====================
    val userId: String,
    var userName: String? = null,
    var userAvatarPath: String? = null,

    // ======API配置列表====================
    var apiConfigs: List<ApiConfig> = emptyList(),

    // ======基础API配置====================
    var currentApiConfigId: String? = null,
    var selectedModel: String? = null,

    // ======图片识别相关配置=================
    var imgRecognitionEnabled: Boolean = false,
    var currentImgApiConfigId: String? = null,
    var selectedImgModel: String? = null,

    // ======其他对话设置=====================
    var maxContextMessageSize: Int = 15,
    var summarizeTriggerCount: Int = 20,
    var maxSummarizeCount: Int = 20,
    var enableSeparator: Boolean = false,
    var enableTimePrefix: Boolean = true,
    var enableStreamOutput: Boolean = true,

    // ======开发者设置=====================
    var showLogcatView: Boolean = false,

    // 选中的角色相关
    var selectedCharacterId: String? = null,
    ){
    val baseUrl get() = apiConfigs.find { it.id == currentApiConfigId }?.baseUrl
    val baseApiKey get() = apiConfigs.find { it.id == currentApiConfigId }?.apiKey

    val imgBaseUrl get() = apiConfigs.find { it.id == currentImgApiConfigId }?.baseUrl
    val imgApiKey get() = apiConfigs.find { it.id == currentImgApiConfigId }?.apiKey
}

@Serializable
data class ApiConfig(
    val id: String = NanoIdUtils.randomNanoId(),
    var name: String = "",           // 配置名称，如 "默认"、"备用"
    var baseUrl: String? = null,     // API 地址
    var apiKey: String? = null,      // 对应的 Key
    var selectedModel: String? = null // 该配置下选中的模型
)
