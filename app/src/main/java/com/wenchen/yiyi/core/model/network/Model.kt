package com.wenchen.yiyi.core.model.network

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

// AI模型列表响应的顶层结构
@Keep
@Serializable
data class ModelsResponse(
    val `object`: String = "list",
    val data: List<Model> = emptyList(),
    val success: Boolean = true
)

// 单个模型的详细信息
@Keep
@Serializable
data class Model(
    val id: String,  // 模型ID，如"gpt-3.5-turbo"
    val `object`: String,
    val created: Long,
    val owned_by: String,
    val permission: List<Permission>? = null,
    val root: String? = null,
    val parent: String? = null
)

// 模型权限信息
@Keep
@Serializable
data class Permission(
    val id: String,
    val `object`: String,
    val created: Long,
    val allow_create_engine: Boolean,
    val allow_sampling: Boolean,
    val allow_logprobs: Boolean,
    val allow_search_indices: Boolean,
    val allow_view: Boolean,
    val allow_fine_tuning: Boolean,
    val organization: String,
    val group: String?,
    val is_blocking: Boolean
)
