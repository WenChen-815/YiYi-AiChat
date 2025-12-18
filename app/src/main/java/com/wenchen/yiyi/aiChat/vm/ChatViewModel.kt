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
import com.wenchen.yiyi.common.ApiService
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
        limitMutableListOf(15)

    var lastGroupChatReply = 0


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
                            characterIds = mapOf((currentAICharacter?.aiCharacterId ?: "") to 1.0f),
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

    fun initGroupChat(conversation: Conversation) {
        // 更新最大上下文消息数
        chatContext.setLimit(configManager.getMaxContextMessageSize())
        try {
            viewModelScope.launch(Dispatchers.IO) {
                val currentCharacters = conversation.characterIds.mapNotNull { (characterId, _) ->
                    aiCharacterDao.getCharacterById(characterId)
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        conversation = conversation,
                        currentCharacters = currentCharacters
                    )
                    loadInitialData()
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "初始化群聊失败", e)
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val initialMessages = chatMessageDao.getMessagesByPage(
                _uiState.value.conversation.id,
                PAGE_SIZE,
                0
            )

            _uiState.value = _uiState.value.copy(
                messages = initialMessages,
                isLoading = false
            )

            currentPage = 1
            val totalCount = chatMessageDao.getTotalMessageCount()
            _uiState.value = _uiState.value.copy(hasMoreData = totalCount > PAGE_SIZE)

            isInitialLoading = false

            tempChatMessageDao.getByConversationId(_uiState.value.conversation.id)
                .takeLast(configManager.getMaxContextMessageSize())
                .let {
                    chatContext.clear()
                    chatContext.addAll(it)
                }
        }
    }

    // 加载更多消息的方法
    fun loadMoreMessages() {
        if (_uiState.value.isLoading || !_uiState.value.hasMoreData || isInitialLoading) return
        Log.d("ChatViewModel", "loadMoreMessages: $currentPage")

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
                    currentMessages.addAll(moreMessages)
                    _uiState.value = _uiState.value.copy(
                        messages = currentMessages
                    )
                    currentPage++

                    // 检查是否还有更多数据
                    val totalCount = chatMessageDao.getTotalMessageCount()
                    _uiState.value =
                        _uiState.value.copy(hasMoreData = (currentPage * PAGE_SIZE) < totalCount)
                } else {
                    _uiState.value = _uiState.value.copy(hasMoreData = false)
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // 添加检查是否可以加载更多消息的方法
    fun canLoadMore(): Boolean = _uiState.value.hasMoreData && !_uiState.value.isLoading

    // 添加获取加载状态的方法
    fun isLoading(): Boolean = _uiState.value.isLoading

    fun continueGenerate(isGroupChat: Boolean) = if (isGroupChat) sendGroupMessage() else sendMessage()

    fun reGenerate(isGroupChat: Boolean){
        val removeMessageIds = mutableListOf<String>()
        if (isGroupChat){
            repeat(lastGroupChatReply) {
                removeMessageIds.add(chatContext.last().id)
                chatContext.removeAt(chatContext.lastIndex)
            }
            sendGroupMessage()
        } else {
            removeMessageIds.add(chatContext.last().id)
            chatContext.removeAt(chatContext.lastIndex)
            sendMessage()
        }
        // 移除ui_state中对应的消息
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages.filter { !removeMessageIds.contains(it.id) }
        )
        // 移除数据库中的消息
        if (removeMessageIds.isNotEmpty()) {
            viewModelScope.launch {
                chatMessageDao.deleteMessagesByIds(removeMessageIds)
                tempChatMessageDao.deleteMessagesByIds(removeMessageIds)
            }
        }
    }
    fun sendMessage(
        messageText: String = "",
        isSendSystemMessage: Boolean = false,
        character: AICharacter? = currentAICharacter
    ) {
//        if (messageText.isEmpty()) return
        _uiState.value = _uiState.value.copy(isAiReplying = true)
        character?.let { character ->
            viewModelScope.launch {
                AIChatManager.sendMessage(
                    conversation = _uiState.value.conversation,
                    aiCharacter = character,
                    newMessageTexts = listOf(messageText),
                    isHandleUserMessage = messageText.isNotEmpty(),
                    oldMessages = chatContext,
                    isSendSystemMessage = isSendSystemMessage,
                )
            }
        }
    }

    fun sendGroupMessage(messageText: String = "", isSendSystemMessage: Boolean = false) {
        if (_uiState.value.conversation.characterIds.isEmpty()) return
        _uiState.value = _uiState.value.copy(isAiReplying = true)
        viewModelScope.launch {
            lastGroupChatReply = AIChatManager.sendGroupMessage(
                conversation = _uiState.value.conversation,
                aiCharacters = _uiState.value.currentCharacters,
                newMessageTexts = listOf(messageText),
                isHandleUserMessage = messageText.isNotEmpty(),
                /**
                 * 知识点：引用传递
                 * 在 ChatViewModel 中，chatContext 是一个 LimitMutableList<TempChatMessage> 对象
                 * 当调用 sendGroupMessage 时，传递的是 chatContext 的引用，而非副本
                 * 因此 AIChatManager.sendGroupMessage 接收到的 oldMessages 参数实际指向同一个对象
                 * 数据同步更新
                 * 在 ChatViewModel.addMessage 方法中，当收到新消息时会调用 chatContext.add(tempMessage)
                 * 这会直接修改 chatContext 对象的内容
                 * 由于 oldMessages 指向同一个对象，所以内容也会同步变化
                 * 并发访问问题
                 * 如果在 AIChatManager.processMessageQueue 等方法处理过程中，chatContext 被更新
                 * oldMessages 也会同步更新, 确保在处理过程中使用的是最新上下文，因此在AIChatManager.sendGroupMessage中<不需要>单独add userMessage在给下一位AI发送消息
                 */
                oldMessages = chatContext,
                isSendSystemMessage = isSendSystemMessage,
            )
        }
    }

    fun sendImage(bitmap: Bitmap, savedUri: Uri, character: AICharacter? = currentAICharacter) {
        _uiState.value = _uiState.value.copy(isAiReplying = true)
        character?.let { character ->
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
        if (message.conversationId != _uiState.value.conversation.id) return
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(0, message)
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
                val conversationId = _uiState.value.conversation.id
                val tag2 = imageManager.deleteAllChatImages(conversationId)
                val tag3 = aiChatMemoryDao.deleteByCharacterIdAndConversationId(
                    character.aiCharacterId,
                    _uiState.value.conversation.id
                ) > 0
                val tag4 = conversationDao.deleteById(conversationId) > 0
                // 最后删除角色
                val tag5 = aiCharacterDao.deleteAICharacter(character) > 0

                // 更新UI状态也在IO线程中执行，避免切换到主线程
                _uiState.value = _uiState.value.copy(
                    currentCharacter = null
                )

                Log.e("ChatViewModel", "删除角色: $tag1 $tag2 $tag3 $tag4 $tag5")
                tag1 && tag2 && tag3 && tag4 && tag5
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "删除角色失败: ${e.message}")
            false
        }
    }

    suspend fun deleteConversation(conversation: Conversation): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // 先删除对话的所有消息
                chatMessageDao.deleteMessagesByConversationId(conversation.id)
                tempChatMessageDao.deleteByConversationId(conversation.id)

                // 删除对话所有相关图片
                val tag1 = imageManager.deleteAllChatImages(conversation.id)
                val tag2 = aiChatMemoryDao.deleteByConversationId(conversation.id) >= 0
                // 最后删除对话
                val tag3 = conversationDao.deleteById(conversation.id) > 0

                Log.e("ChatViewModel", "删除对话: $tag1 $tag2 $tag3")
                tag1 && tag2 && tag3
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "删除对话失败: ${e.message}")
            false
        }
    }

    fun setReceiveNewMessage(receiveNewMessage: Boolean) {
        _uiState.value = _uiState.value.copy(receiveNewMessage = receiveNewMessage)
    }

    fun updateMessageContent(messageId: String, content: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                // 更新数据库
                chatMessageDao.updateMessageContent(messageId, content)
                tempChatMessageDao.updateMessageContent(messageId, content)

                // 更新UI状态中的消息列表
                _uiState.update { currentState ->
                    val updatedMessages = currentState.messages.map { message ->
                        if (message.id == messageId) message.copy(content = content) else message
                    }
                    currentState.copy(messages = updatedMessages)
                }
                chatContext.forEach { message ->
                    if (message.id == messageId) {
                        message.content = content
                    }
                }

                onComplete?.invoke(true)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "修改消息内容失败: ${e.message}", e)
                onComplete?.invoke(false)
            }
        }
    }

    fun deleteOneMessage(messageId: String) {
        viewModelScope.launch {
            try {
                // 删除数据库中的消息
                chatMessageDao.deleteMessageById(messageId)
                tempChatMessageDao.deleteMessageById(messageId)

                // 更新UI状态中的消息列表
                _uiState.update { currentState ->
                    val updatedMessages = currentState.messages.filter { it.id != messageId }
                    currentState.copy(messages = updatedMessages)
                }
                chatContext.removeIf { it.id == messageId }

                Log.e("ChatViewModel", "删除消息: $messageId")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "删除消息失败: ${e.message}", e)
            }
        }
    }
}

data class ChatUiState(
    val conversation: Conversation = Conversation(
        id = "",
        name = "",
        type = ConversationType.SINGLE,
        characterIds = emptyMap(),
        characterKeywords = emptyMap(),
        playerName = "",
        playGender = "",
        playerDescription = "",
        chatWorldId = "",
        chatSceneDescription = "",
        additionalSummaryRequirement = "",
        avatarPath = "",
        backgroundPath = ""
    ),
    val messages: List<ChatMessage> = emptyList(),
    val currentCharacter: AICharacter? = null,
    val currentCharacters: List<AICharacter> = emptyList(),
    val currentModelName: String = "",
    val isLoading: Boolean = false,
    val isAiReplying: Boolean = false,
    val hasMoreData: Boolean = true,
    val receiveNewMessage: Boolean = false
)
