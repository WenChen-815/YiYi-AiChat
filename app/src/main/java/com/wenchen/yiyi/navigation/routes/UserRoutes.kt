package com.wenchen.yiyi.navigation.routes

import kotlinx.serialization.Serializable

/**
 * 用户相关路由
 */
object UserRoutes {

    /**
     * 用户信息页
     * 登录后才能访问的用户信息页
     */
    @Serializable
    data object Info
}
