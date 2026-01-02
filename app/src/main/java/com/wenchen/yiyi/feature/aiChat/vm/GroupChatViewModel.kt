package com.wenchen.yiyi.feature.aiChat.vm

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.feature.aiChat.entity.Conversation
import com.wenchen.yiyi.feature.aiChat.entity.ConversationType
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
    navigator: AppNavigator,
    userState: UserState,
    savedStateHandle: SavedStateHandle
): BaseChatViewModel(
    navigator = navigator,
    userState = userState,
    savedStateHandle = savedStateHandle
){
    init {
        val route = savedStateHandle.toRoute<AiChatRoutes.GroupChat>()
        val conversationId = route.conversationId
        viewModelScope.launch(Dispatchers.IO) {
            initGroupChat(conversationId)
        }
        // 更新最大上下文消息数
        chatContext.setLimit(configManager.getMaxContextMessageSize())
        init()
    }

    fun initGroupChat(conversationId: String) {

        try {
            viewModelScope.launch(Dispatchers.IO) {
                val conversation = conversationDao.getById(conversationId)
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
                    aiCharacterDao.getCharacterById(characterId)
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
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