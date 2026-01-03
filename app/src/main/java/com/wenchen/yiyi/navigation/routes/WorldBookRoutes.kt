package com.wenchen.yiyi.navigation.routes

import kotlinx.serialization.Serializable

object WorldBookRoutes {
    /**
     * 世界列表页
     */
    @Serializable
    data object WorldBookList
    /**
     * 世界编辑页
     */
    @Serializable
    data class WorldBookEdit(val worldId: String, val isNewWorld: Boolean = false)
}