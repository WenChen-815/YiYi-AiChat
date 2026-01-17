package com.wenchen.yiyi.feature.output.model

import kotlinx.serialization.Serializable

@Serializable
class CharaExtensionModel (
    val memory: String? = null, // 记忆
    val avatarByte: ByteArray? = null, // 角色头像
    val backgroundByte: ByteArray? = null // 角色背景
)