package com.wenchen.yiyi.navigation.routes

import kotlinx.serialization.Serializable

/**
 * 聊天路由
 */
object ChatRoutes {

    /**
     * 单聊页
     */
    @Serializable
    data object SingleChat

    /**
     * 群聊页
     */
    @Serializable
    data object GroupChat

     /**
      * 会话设置页
      */
    @Serializable
    data object ConversationSetting
}