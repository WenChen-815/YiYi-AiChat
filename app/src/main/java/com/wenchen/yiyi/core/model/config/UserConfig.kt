package com.wenchen.yiyi.core.model.config

import kotlinx.serialization.Serializable

@Serializable
data class UserConfig(
    // ======用户相关配置====================
    val userId: String? = null,
    var userName: String? = null,
    var userAvatarPath: String? = null,

    // ======基础API配置====================
    var baseApiKey: String? = null,
    var baseUrl: String? = null,
    var selectedModel: String? = null,

    // ======图片识别相关配置=================
    var imgRecognitionEnabled: Boolean = false,
    var imgApiKey: String? = null,
    var imgBaseUrl: String? = null,
    var selectedImgModel: String? = null,

    // ======其他对话设置=====================
    var maxContextMessageSize: Int = 15,
    var summarizeTriggerCount: Int = 20,
    var maxSummarizeCount: Int = 20,
    var enableSeparator: Boolean = false,
    var enableTimePrefix: Boolean = true,

    // 选中的角色相关
    var selectedCharacterId: String? = null,
)
