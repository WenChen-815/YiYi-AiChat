package com.wenchen.yiyi.feature.aiChat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wenchen.yiyi.core.data.repository.*
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.core.util.ui.ToastUtils
import com.wenchen.yiyi.feature.aiChat.common.AIChatManager
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.routes.AiChatRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SingleChatViewModel @Inject constructor(
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
        val route = savedStateHandle.toRoute<AiChatRoutes.SingleChat>()
        val conversationId = route.conversationId
        viewModelScope.launch(Dispatchers.IO) {
            initChat(conversationId)
        }
        // 更新最大上下文消息数
        chatContext.setLimit(userConfigState.userConfig.value?.maxContextMessageSize ?: 15)
        init()
    }

    suspend fun initChat(characterId: String) {
        try {
            if (characterId.isNotEmpty()) {
                aiCharacterRepository.getCharacterById(characterId)
                    ?.let { character ->
                        currentAICharacter = character
                        // 初始化会话ID
                        val conversationId = "${userConfigState.userConfig.value?.userId}_${currentAICharacter?.id}"
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
                                chatWorldId = "",
                                chatSceneDescription = "",
                                additionalSummaryRequirement = "",
                                avatarPath = currentAICharacter?.avatar,
                                backgroundPath = currentAICharacter?.background,
                            )
                        withContext(Dispatchers.Main) {
                            _conversation.value = conversation
                            updateCharacterDisplay()
                            loadInitialData()
                        }
                    }
            }
        } catch (e: Exception) {
            Timber.tag("SingleChatViewModel").e(e, "解析角色JSON失败")
        }
    }
    private fun updateCharacterDisplay() {
        currentAICharacter?.let { character ->
            _chatUiState.value = _chatUiState.value.copy(
                currentCharacter = character,
            )
        }
    }

    fun deleteCharacter(character: AICharacter, callback: (Boolean) -> Unit) {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                // 先删除角色的所有消息
                chatMessageRepository.deleteMessagesByConversationId(conversation.value.id)
                tempChatMessageRepository.deleteByConversationId(conversation.value.id)

                // 删除角色所有相关图片
                val tag1 = imageManager.deleteAllCharacterImages(character)
                val conversationId = conversation.value.id
                val tag2 = imageManager.deleteAllChatImages(conversationId)
                val tag3 = aiChatMemoryRepository.deleteByCharacterIdAndConversationId(
                    character.id,
                    conversation.value.id
                ) > 0
                val tag4 = conversationRepository.deleteById(conversationId) > 0
                // 最后删除角色
                val tag5 = aiCharacterRepository.deleteAICharacter(character) > 0

                val result = tag1 && tag2 && tag3 && tag4 && tag5
                // 更新UI状态
                withContext(Dispatchers.Main) {
                    _chatUiState.value = _chatUiState.value.copy(
                        currentCharacter = null
                    )
                    if (result) {
                        ToastUtils.showToast("角色删除成功")
                    } else {
                        ToastUtils.showToast("角色删除失败")
                    }
                }

                Timber.tag("SingleChatViewModel").e("删除角色: $tag1 $tag2 $tag3 $tag4 $tag5")
                callback(result)
                navigateBack()
            }
        } catch (e: Exception) {
            Timber.tag("SingleChatViewModel").e(e, "删除角色失败: ${e.message}")
            callback(false)
        }
    }

    fun refreshCharacterInfo() {
        currentAICharacter?.let { character ->
            viewModelScope.launch(Dispatchers.IO) {
                aiCharacterRepository.getCharacterById(character.id)
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