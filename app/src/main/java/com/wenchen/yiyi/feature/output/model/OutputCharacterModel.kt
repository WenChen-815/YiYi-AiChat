package com.wenchen.yiyi.feature.output.model

import kotlinx.serialization.Serializable

@Serializable
class OutputCharacterModel (
    val name: String,
    val roleIdentity: String? = null, // 角色身份
    val roleAppearance: String? = null, // 角色外观
    val roleDescription: String? = null, // 角色描述
    val outputExample: String? = null, // 输出示例
    val behaviorRules: String? = null, // 行为规则
    val memory: String? = null, // 记忆
    val avatarByte: ByteArray? = null, // 角色头像
    val backgroundByte: ByteArray? = null // 角色背景
)