package com.wenchen.yiyi.navigation.routes

import com.wenchen.yiyi.core.common.entity.AICharacter
import kotlinx.serialization.Serializable

/**
 * 聊天路由
 */
object AiChatRoutes {

    /**
     * 单聊页
     */
    @Serializable
    data class SingleChat(val conversationId: String)

    /**
     * 群聊页
     */
    @Serializable
    data class GroupChat(val conversationId: String)

    /**
     * 角色创建/编辑页
     */
    @Serializable
    data class CharacterEdit(val characterId :String, val isNewCharacter: Boolean = false)

    /**
     * 会话设置页
     */
    @Serializable
    data class ConversationSetting(val conversationId: String)
}