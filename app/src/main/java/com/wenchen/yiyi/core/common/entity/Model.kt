package com.wenchen.yiyi.core.common.entity

// AI模型列表响应的顶层结构
data class ModelsResponse(
    val `object`: String,
    val data: List<Model>,
    val success: Boolean
)

// 单个模型的详细信息
data class Model(
    val id: String,  // 模型ID，如"gpt-3.5-turbo"
    val `object`: String,
    val created: Long,
    val owned_by: String,
    val permission: List<Permission>?,
    val root: String?,
    val parent: String?
)

// 模型权限信息
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
