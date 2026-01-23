package com.wenchen.yiyi.navigation.routes

import kotlinx.serialization.Serializable

/**
 * 正则路由
 */
object RegexRoutes {
    /**
     * 正则分组配置页
     */
    @Serializable
    data object RegexConfig

    /**
     * 正则详情页
     * @param groupId 分组 ID
     */
    @Serializable
    data class RegexDetail(val groupId: String)
}