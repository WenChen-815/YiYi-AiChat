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

    /**
     * 世界词条编辑页
     */
    @Serializable
    data class WorldBookEntryEdit(val worldId: String, val entryId: String? = null)
}
