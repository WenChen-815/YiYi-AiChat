package com.wenchen.yiyi.navigation.routes

import kotlinx.serialization.Serializable

/**
 * 全局配置路由
 */
object ConfigRoutes {

    /**
     * 配置主页（导航中心）
     */
    @Serializable
    data object Settings

    /**
     * 全局聊天设置页
     */
    @Serializable
    data object ChatConfig

    /**
     * API 线路与模型配置页
     */
    @Serializable
    data object ApiConfig

    /**
     * 正则表达式分组配置页
     */
    @Serializable
    data object RegexConfig

    /**
     * 正则表达式详情页
     * @param groupId 分组 ID
     */
    @Serializable
    data class RegexDetail(val groupId: String)
}
