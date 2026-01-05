package com.wenchen.yiyi.feature.aiChat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.routes.AiChatRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SingleChatViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    savedStateHandle: SavedStateHandle
): BaseChatViewModel(
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
                aiCharacterDao.getCharacterById(characterId)
                    ?.let { character ->
                        currentAICharacter = character
                        // 初始化会话ID
                        val conversationId = "${userConfigState.userConfig.value?.userId}_${currentAICharacter?.aiCharacterId}"
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
            Timber.tag("SingleChatViewModel").e(e, "解析角色JSON失败")
        }
    }
    private fun updateCharacterDisplay() {
        currentAICharacter?.let { character ->
            _uiState.value = _uiState.value.copy(
                currentCharacter = character,
            )
        }
    }

    fun deleteCharacter(character: AICharacter): Boolean {
        return try {
            runBlocking(Dispatchers.IO) {
                // 先删除角色的所有消息
                chatMessageDao.deleteMessagesByConversationId(conversation.value.id)
                tempChatMessageDao.deleteByConversationId(conversation.value.id)

                // 删除角色所有相关图片
                val tag1 = imageManager.deleteAllCharacterImages(character)
                val conversationId = conversation.value.id
                val tag2 = imageManager.deleteAllChatImages(conversationId)
                val tag3 = aiChatMemoryDao.deleteByCharacterIdAndConversationId(
                    character.aiCharacterId,
                    conversation.value.id
                ) > 0
                val tag4 = conversationDao.deleteById(conversationId) > 0
                // 最后删除角色
                val tag5 = aiCharacterDao.deleteAICharacter(character) > 0

                // 更新UI状态
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        currentCharacter = null
                    )
                }

                Timber.tag("SingleChatViewModel").e("删除角色: $tag1 $tag2 $tag3 $tag4 $tag5")
                tag1 && tag2 && tag3 && tag4 && tag5
            }
        } catch (e: Exception) {
            Timber.tag("SingleChatViewModel").e(e, "删除角色失败: ${e.message}")
            false
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