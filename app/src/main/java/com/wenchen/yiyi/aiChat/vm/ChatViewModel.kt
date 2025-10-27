package com.wenchen.yiyi.aiChat.vm

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.wenchen.yiyi.aiChat.entity.ChatMessage
import com.wenchen.yiyi.aiChat.entity.TempChatMessage
import com.wenchen.yiyi.common.App
import com.wenchen.yiyi.common.entity.AICharacter
import com.wenchen.yiyi.aiChat.common.AIChatManager
import com.wenchen.yiyi.config.common.ConfigManager
import com.wenchen.yiyi.aiChat.common.ApiService
import com.wenchen.yiyi.aiChat.common.ImageManager
import com.wenchen.yiyi.aiChat.entity.Conversation
import com.wenchen.yiyi.aiChat.entity.ConversationType
import com.wenchen.yiyi.common.entity.Model
import com.wenchen.yiyi.common.utils.LimitMutableList
import com.wenchen.yiyi.common.utils.limitMutableListOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.toMutableList
import kotlin.jvm.java

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private val configManager = ConfigManager()

    private val imageManager = ImageManager()
    private var apiService: ApiService? = null
    private var selectedModel: String = ""
    private var supportedModels: List<Model> = emptyList()

    private var isInitialLoading = false

    private val chatMessageDao = App.appDatabase.chatMessageDao()
    private val tempChatMessageDao = App.appDatabase.tempChatMessageDao()
    private val aiCharacterDao = App.appDatabase.aiCharacterDao()
    private val aiChatMemoryDao = App.appDatabase.aiChatMemoryDao()
    private val conversationDao = App.appDatabase.conversationDao()

    private val PAGE_SIZE = 15
    private var currentPage = 0

    private var currentAICharacter: AICharacter? = null
    private var chatContext: LimitMutableList<TempChatMessage> =
        limitMutableListOf<TempChatMessage>(10)

    init {
        initApiService()
    }

    fun initChat(characterJson: String?, characterId: String?) {
        // 更新最大上下文消息数
        chatContext.setLimit(configManager.getMaxContextMessageSize())
        try {
            if (!characterJson.isNullOrEmpty()) {
                currentAICharacter = Gson().fromJson(characterJson, AICharacter::class.java)
                // 初始化会话ID
                val conversationId =
                    "${configManager.getUserId().toString()}_${currentAICharacter?.aiCharacterId}"
                viewModelScope.launch(Dispatchers.IO) {
                    val conversation = conversationDao.getById(conversationId)
                        ?: Conversation(
                            id = conversationId,
                            name = currentAICharacter?.name ?: "未获取到对话信息",
                            type = ConversationType.SINGLE,
                            characterIds = listOf(currentAICharacter?.aiCharacterId ?: ""),
                            playerName = "",
                            playGender = "",
                            playerDescription = "",
                            chatWorldId = "",
                            chatSceneDescription = "",
                            avatarPath = currentAICharacter?.avatarPath,
                            backgroundPath = currentAICharacter?.backgroundPath,
                        )

                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            conversation = conversation
                        )
                        updateCharacterDisplay()
                        loadInitialData()
                    }
                }
                return
            }

            val id = characterId ?: configManager.getSelectedCharacterId() ?: ""
            if (id.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    App.appDatabase.aiCharacterDao().getCharacterById(id)
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
                                    characterIds = listOf(currentAICharacter?.aiCharacterId ?: ""),
                                    playerName = "",
                                    playGender = "",
                                    playerDescription = "",
                                    chatWorldId = "",
                                    chatSceneDescription = "",
                                    avatarPath = currentAICharacter?.avatarPath,
                                    backgroundPath = currentAICharacter?.backgroundPath,
                                )
                            withContext(Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(
                                    conversation = conversation
                                )
                                updateCharacterDisplay()
                                loadInitialData()
                            }
                        }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "解析角色JSON失败", e)
        }
    }

    private fun initApiService() {
        val apiKey = configManager.getApiKey() ?: ""
        val baseUrl = configManager.getBaseUrl() ?: ""
        selectedModel = configManager.getSelectedModel() ?: ""
        apiService = ApiService(baseUrl, apiKey)
        loadSupportedModels()
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
                App.appDatabase.aiCharacterDao().getCharacterById(character.aiCharacterId)
                    ?.let { updatedCharacter ->
                        currentAICharacter = updatedCharacter
                        withContext(Dispatchers.Main) {
                            updateCharacterDisplay()
                        }
                    }
            }
        }
    }

    fun refreshConversationInfo() {
        viewModelScope.launch {
            try {
                val updatedConversation = conversationDao.getById(_uiState.value.conversation.id)
                if (updatedConversation != null) {
                    _uiState.update { it.copy(conversation = updatedConversation) }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "刷新对话信息失败", e)
            }
        }
    }


    private fun loadSupportedModels() {
        apiService?.let { service ->
            viewModelScope.launch(Dispatchers.IO) {
                service.getSupportedModels(
                    onSuccess = { models ->
                        supportedModels = models
                        _uiState.value = _uiState.value.copy(
                            currentModelName = selectedModel
                        )
                    },
                    onError = { errorMsg ->
                        // 错误处理
                    }
                )
            }
        }
    }

    private fun loadInitialData() {
        if (isInitialLoading) return
        isInitialLoading = true

        Log.d("ChatViewModel", "loadInitialData: ${_uiState.value.conversation.id}")
        currentAICharacter?.let { character ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val initialMessages = chatMessageDao.getMessagesByPage(
                    _uiState.value.conversation.id,
                    PAGE_SIZE,
                    0
                )

                val reversedMessages = initialMessages.reversed()
                _uiState.value = _uiState.value.copy(
                    messages = initialMessages,
                    isLoading = false
                )

                currentPage = 1
                val totalCount = chatMessageDao.getTotalMessageCount()
                _uiState.value = _uiState.value.copy(hasMoreData = totalCount > PAGE_SIZE)

                isInitialLoading = false

                tempChatMessageDao.getByCharacterId(character.aiCharacterId)
                    .takeLast(configManager.getMaxContextMessageSize())
                    .let {
                        chatContext.clear()
                        chatContext.addAll(it)
                    }
            }
        }
    }

    // 加载更多消息的方法
    fun loadMoreMessages() {
        if (_uiState.value.isLoading || !_uiState.value.hasMoreData || isInitialLoading) return
        Log.d("ChatViewModel", "loadMoreMessages: $currentPage")

        currentAICharacter?.let { character ->
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val offset = currentPage * PAGE_SIZE
                val moreMessages = chatMessageDao.getMessagesByPage(
                    _uiState.value.conversation.id,
                    PAGE_SIZE,
                    offset
                )

                withContext(Dispatchers.Main) {
                    if (moreMessages.isNotEmpty()) {
                        val currentMessages = _uiState.value.messages.toMutableList()
                        // 将新消息添加到列表开头（因为是历史消息）
//                        currentMessages.addAll(0, moreMessages.reversed())
                        currentMessages.addAll(moreMessages)
                        _uiState.value = _uiState.value.copy(
                            messages = currentMessages
                        )
                        currentPage++

                        // 检查是否还有更多数据
                        val totalCount = chatMessageDao.getTotalMessageCount()
                        _uiState.value = _uiState.value.copy(hasMoreData = (currentPage * PAGE_SIZE) < totalCount)
                    } else {
                        _uiState.value = _uiState.value.copy(hasMoreData = false)
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    // 添加检查是否可以加载更多消息的方法
    fun canLoadMore(): Boolean = _uiState.value.hasMoreData && !_uiState.value.isLoading

    // 添加获取加载状态的方法
    fun isLoading(): Boolean = _uiState.value.isLoading

    fun sendMessage(messageText: String, isSendSystemMessage: Boolean) {
        if (messageText.isEmpty()) return
        _uiState.value = _uiState.value.copy(isAiReplying = true) // 显示进度条

        currentAICharacter?.let { character ->
            val tempChatContext = chatContext
            viewModelScope.launch {
                AIChatManager.sendMessage(
                    conversation = _uiState.value.conversation,
                    aiCharacter = character,
                    newMessageTexts = listOf(messageText),
                    oldMessages = tempChatContext,
                    isSendSystemMessage = isSendSystemMessage,
                )
            }
        }
    }

    fun sendImage(bitmap: Bitmap, savedUri: Uri) {
        _uiState.value = _uiState.value.copy(isAiReplying = true) // 显示进度条
        currentAICharacter?.let { character ->
            val tempChatContext = chatContext
            viewModelScope.launch {
                AIChatManager.sendImage(
                    conversation = _uiState.value.conversation,
                    character,
                    bitmap,
                    savedUri,
                    tempChatContext
                )
            }
        }
    }

    fun addMessage(message: ChatMessage) {
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(0,message)
        _uiState.value = _uiState.value.copy(
            messages = currentMessages
        )

        // 添加到聊天上下文中
        if (message.imgUrl == null && message.voiceUrl == null) {
            val tempMessage = TempChatMessage(
                id = message.id,
                content = message.content,
                type = message.type,
                characterId = message.characterId,
                chatUserId = message.chatUserId,
                contentType = message.contentType,
                timestamp = message.timestamp,
                isShow = message.isShow,
                conversationId = _uiState.value.conversation.id
            )
            chatContext.add(tempMessage)
        }
    }

    fun clearChatHistory() {
        currentAICharacter?.let { character ->
            viewModelScope.launch(Dispatchers.IO) {
                chatMessageDao.deleteMessagesByConversationId(_uiState.value.conversation.id)
                tempChatMessageDao.deleteByConversationId(_uiState.value.conversation.id)

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        messages = emptyList()
                    )
                    chatContext.clear()
                }
            }
        }
    }

    fun selectModel(modelId: String) {
        selectedModel = modelId
        configManager.saveSelectedModel(selectedModel)
        _uiState.value = _uiState.value.copy(
            currentModelName = selectedModel
        )
    }

    fun getSupportedModels(): List<Model> = supportedModels

    fun getCurrentCharacter(): AICharacter? = currentAICharacter

    fun getConfigManager(): ConfigManager = configManager

    fun hideProgress() {
        _uiState.value = _uiState.value.copy(isAiReplying = false)
    }

    suspend fun deleteCharacter(character: AICharacter): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // 先删除角色的所有消息
                chatMessageDao.deleteMessagesByConversationId(_uiState.value.conversation.id)
                tempChatMessageDao.deleteByConversationId(_uiState.value.conversation.id)

                // 删除角色所有相关图片
                val tag1 = imageManager.deleteAllCharacterImages(character)
                val conversationId =
                    "${configManager.getUserId().toString()}_${character.aiCharacterId}"
                val tag2 = imageManager.deleteAllChatImages(conversationId)
                val tag3 = aiChatMemoryDao.deleteByCharacterIdAndConversationId(
                    character.aiCharacterId,
                    _uiState.value.conversation.id
                ) > 0
                // 最后删除角色
                val tag4 = aiCharacterDao.deleteAICharacter(character) > 0

                // 更新UI状态也在IO线程中执行，避免切换到主线程
                _uiState.value = _uiState.value.copy(
                    currentCharacter = null
                )

                Log.e("ChatViewModel", "删除角色: $tag1 $tag2 $tag3")
                tag1 && tag2 && tag3 && tag4
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "删除角色失败: ${e.message}")
            false
        }
    }

    fun setReceiveNewMessage(receiveNewMessage: Boolean) {
        _uiState.value = _uiState.value.copy(receiveNewMessage = receiveNewMessage)
    }
}

data class ChatUiState(
    val conversation: Conversation = Conversation(
        id = "",
        name = "",
        type = ConversationType.SINGLE,
        characterIds = emptyList(),
        playerName = "",
        playGender = "",
        playerDescription = "",
        chatWorldId = "",
        chatSceneDescription = "",
        avatarPath = "",
        backgroundPath = ""
    ),
    val messages: List<ChatMessage> = emptyList(),
    val currentCharacter: AICharacter? = null,
    val currentModelName: String = "",
    val isLoading: Boolean = false,
    val isAiReplying: Boolean = false,
    val hasMoreData: Boolean = true,
    val receiveNewMessage: Boolean = false
)
