package com.wenchen.yiyi.feature.aiChat.vm

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wenchen.yiyi.Application
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

@HiltViewModel
class SingleChatViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    savedStateHandle: SavedStateHandle
): BaseChatViewModel(
    navigator = navigator,
    userState = userState,
    savedStateHandle = savedStateHandle
){

    init {
        val route = savedStateHandle.toRoute<AiChatRoutes.SingleChat>()
        val conversationId = route.conversationId
        viewModelScope.launch(Dispatchers.IO) {
            initChat(conversationId)
        }
        // 更新最大上下文消息数
        chatContext.setLimit(configManager.getMaxContextMessageSize())
        init()
    }

    suspend fun initChat(characterId: String) {
        try {
            if (characterId.isNotEmpty()) {
                aiCharacterDao.getCharacterById(characterId)
                    ?.let { character ->
                        currentAICharacter = character
                        // 初始化会话ID
                        val conversationId = "${
                            configManager.getUserId().toString()
                        }_${currentAICharacter?.aiCharacterId}"
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
                        withContext(Dispatchers.Main) {
                            _conversation.value = conversation
                            updateCharacterDisplay()
                            loadInitialData()
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "解析角色JSON失败", e)
        }
    }
    private fun updateCharacterDisplay() {
        currentAICharacter?.let { character ->
            _uiState.value = _uiState.value.copy(
                currentCharacter = character,
            )
        }
    }

    fun refreshCharacterInfo() {
        currentAICharacter?.let { character ->
            viewModelScope.launch(Dispatchers.IO) {
                Application.appDatabase.aiCharacterDao().getCharacterById(character.aiCharacterId)
                    ?.let { updatedCharacter ->
                        currentAICharacter = updatedCharacter
                        withContext(Dispatchers.Main) {
                            updateCharacterDisplay()
                        }
                    }
            }
        }
    }
}