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
    val `object`: String? = null,
    val created: Long? = null,
    val owned_by: String? = null,
    val permission: List<Permission>? = null,
    val root: String? = null,
    val parent: String? = null
)

// 模型权限信息
@Keep
@Serializable
data class Permission(
    val id: String,
    val `object`: String? = null,
    val created: Long? = null,
    val allow_create_engine: Boolean? = null,
    val allow_sampling: Boolean? = null,
    val allow_logprobs: Boolean? = null,
    val allow_search_indices: Boolean? = null,
    val allow_view: Boolean? = null,
    val allow_fine_tuning: Boolean? = null,
    val organization: String? = null,
    val group: String? = null,
    val is_blocking: Boolean? = null
)
