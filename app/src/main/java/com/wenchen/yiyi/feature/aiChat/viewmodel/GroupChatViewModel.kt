package com.wenchen.yiyi.feature.aiChat.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wenchen.yiyi.core.data.repository.*
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.feature.aiChat.common.AIChatManager
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.routes.AiChatRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                            (currentAICharacter?.aiCharacterId ?: "") to 1.0f
                        ),
                        characterKeywords = mapOf((currentAICharacter?.aiCharacterId ?: "") to emptyList()),
                        playerName = "",
                        playGender = "",
                        playerDescription = "",
                        chatWorldId = "",
                        chatSceneDescription = "",
                        additionalSummaryRequirement = "",
                        avatarPath = currentAICharacter?.avatarPath,
                        backgroundPath = currentAICharacter?.backgroundPath,
                    )
                val currentCharacters = conversation.characterIds.mapNotNull { (characterId, _) ->
                    aiCharacterRepository.getCharacterById(characterId)
                }
                withContext(Dispatchers.Main) {
                    _chatUiState.value = _chatUiState.value.copy(
                        currentCharacters = currentCharacters
                    )
                    _conversation.value = conversation
                    loadInitialData()
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "初始化群聊失败", e)
        }
    }
}