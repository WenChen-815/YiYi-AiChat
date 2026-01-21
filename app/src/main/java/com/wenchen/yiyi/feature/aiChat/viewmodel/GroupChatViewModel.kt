package com.wenchen.yiyi.feature.aiChat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wenchen.yiyi.core.data.repository.*
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.core.util.business.ChatUtils
import com.wenchen.yiyi.feature.aiChat.common.AIChatManager
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.routes.AiChatRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.component1
import kotlin.collections.component2

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    conversationRepository: ConversationRepository,
    chatMessageRepository: ChatMessageRepository,
    tempChatMessageRepository: TempChatMessageRepository,
    aiCharacterRepository: AICharacterRepository,
    aiChatMemoryRepository: AIChatMemoryRepository,
    aiHubRepository: AiHubRepository,
    regexScriptRepository: YiYiRegexScriptRepository,
    chatUtils: ChatUtils,
    aiChatManager: AIChatManager,
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    savedStateHandle: SavedStateHandle
): BaseChatViewModel(
    conversationRepository = conversationRepository,
    chatMessageRepository = chatMessageRepository,
    tempChatMessageRepository = tempChatMessageRepository,
    aiCharacterRepository = aiCharacterRepository,
    aiChatMemoryRepository = aiChatMemoryRepository,
    aiHubRepository = aiHubRepository,
    regexScriptRepository = regexScriptRepository,
    chatUtils = chatUtils,
    aiChatManager = aiChatManager,
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState,
    savedStateHandle = savedStateHandle
){
    init {
        val route = savedStateHandle.toRoute<AiChatRoutes.GroupChat>()
        val conversationId = route.conversationId
        viewModelScope.launch(Dispatchers.IO) {
            initGroupChat(conversationId)
        }
        // 更新最大上下文消息数
        chatContext.setLimit(userConfigState.userConfig.value?.maxContextMessageSize ?: 15)
        init()
    }

    fun initGroupChat(conversationId: String) {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                val conversation = conversationRepository.getById(conversationId)
                    ?: Conversation(
                        id = conversationId,
                        name = currentAICharacter?.name ?: "未获取到对话信息",
                        type = ConversationType.SINGLE,
                        characterIds = mapOf(
                            (currentAICharacter?.id ?: "") to 1.0f
                        ),
                        characterKeywords = mapOf((currentAICharacter?.id ?: "") to emptyList()),
                        playerName = "",
                        playGender = "",
                        playerDescription = "",
                        chatWorldId = emptyList(),
                        chatSceneDescription = "",
                        additionalSummaryRequirement = "",
                        avatarPath = currentAICharacter?.avatar,
                        backgroundPath = currentAICharacter?.background,
                    )
                val currentCharacters = conversation.characterIds.mapNotNull { (characterId, _) ->
                    aiCharacterRepository.getCharacterById(characterId)
                }
                _regexList.value = regexScriptRepository.getScriptsByGroupIds(conversation.enabledRegexGroups ?: emptyList())
                withContext(Dispatchers.Main) {
                    _chatUiState.value = _chatUiState.value.copy(
                        currentCharacters = currentCharacters
                    )
                    _conversation.value = conversation
                    loadInitialData()
                }
            }
        } catch (e: Exception) {
            Timber.tag("ChatViewModel").e(e, "初始化群聊失败")
        }
    }
}