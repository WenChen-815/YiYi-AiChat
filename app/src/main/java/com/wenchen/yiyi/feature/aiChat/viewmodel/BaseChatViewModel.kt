package com.wenchen.yiyi.feature.aiChat.viewmodel

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.core.base.state.BaseNetWorkUiState
import com.wenchen.yiyi.core.database.entity.ChatMessage
import com.wenchen.yiyi.core.database.entity.TempChatMessage
import com.wenchen.yiyi.core.base.viewmodel.BaseNetWorkViewModel
import com.wenchen.yiyi.core.data.repository.*
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.feature.aiChat.common.AIChatManager
import com.wenchen.yiyi.core.datastore.storage.ImageManager
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.core.database.entity.YiYiRegexScript
import com.wenchen.yiyi.core.model.network.Model
import com.wenchen.yiyi.core.model.network.ModelsResponse
import com.wenchen.yiyi.core.model.network.NetworkResponse
import com.wenchen.yiyi.core.result.ResultHandler
import com.wenchen.yiyi.core.result.asResult
import com.wenchen.yiyi.core.util.common.LimitMutableList
import com.wenchen.yiyi.core.util.common.limitMutableListOf
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.util.business.ChatUtils
import com.wenchen.yiyi.core.util.ui.ToastUtils
import com.wenchen.yiyi.navigation.AppNavigator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

abstract class BaseChatViewModel(
    protected val conversationRepository: ConversationRepository,
    protected val chatMessageRepository: ChatMessageRepository,
    protected val tempChatMessageRepository: TempChatMessageRepository,
    protected val aiCharacterRepository: AICharacterRepository,
    protected val aiChatMemoryRepository: AIChatMemoryRepository,
    private val aiHubRepository: AiHubRepository,
    protected val regexScriptRepository: YiYiRegexScriptRepository,
    protected val chatUtils: ChatUtils,
    private val aiChatManager: AIChatManager,
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    savedStateHandle: SavedStateHandle
): BaseNetWorkViewModel<ModelsResponse>(
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState,
) {
    val _chatUiState = MutableStateFlow(ChatUiState())
    val chatUiState: StateFlow<ChatUiState> = _chatUiState.asStateFlow()

    val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val _regexList = MutableStateFlow<List<YiYiRegexScript>>(emptyList())
    val regexList: StateFlow<List<YiYiRegexScript>> = _regexList.asStateFlow()

//    val _messageItemHeights = MutableStateFlow<Map<String, Int>>(emptyMap())
//    val messageItemHeights: StateFlow<Map<String, Int>> = _messageItemHeights.asStateFlow()

    private val _messageSegmentHeights = MutableStateFlow<Map<String, Map<Int, Int>>>(emptyMap())
    val messageSegmentHeights: StateFlow<Map<String, Map<Int, Int>>> = _messageSegmentHeights.asStateFlow()

    var currentAICharacter: AICharacter? = null

    val imageManager = ImageManager()
    private var selectedModel: String = ""
    private var supportedModels: List<Model> = emptyList()

    private var isInitialLoading = false

    private val PAGE_SIZE = 5
    private var currentPage = 0
    var chatContext: LimitMutableList<TempChatMessage> =
        limitMutableListOf(15)

    var lastGroupChatReply = 0

    val _conversation = MutableStateFlow(Conversation(
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
    ))
    val conversation: StateFlow<Conversation> = _conversation.asStateFlow()

    override fun requestApiFlow(): Flow<NetworkResponse<ModelsResponse>> {
        return aiHubRepository.getModels()
    }

    init {
        userConfigState.userConfig.onEach {
            initApi()
        }.launchIn(viewModelScope)
    }

    fun init() {
        setupChatListener()
    }

    private fun initApi() {
        val userConfig = userConfigState.userConfig.value
        val apiKey = userConfig?.baseApiKey ?: ""
        val baseUrl = userConfig?.baseUrl ?: ""
        selectedModel = userConfig?.selectedModel ?: ""
        loadSupportedModels(baseUrl, apiKey)
    }
    /**
     * 记录消息片段的高度
     * @param messageId 消息唯一ID
     * @param segmentIndex 片段索引
     * @param height 高度(px)
     */
    fun rememberSegmentHeight(messageId: String, segmentIndex: Int, height: Int) {
        _messageSegmentHeights.update { current ->
            val existingSegments = current[messageId] ?: emptyMap()
            current + (messageId to (existingSegments + (segmentIndex to height)))
        }
    }

    /**
     * 获取消息片段的高度
     */
    fun findSegmentHeight(messageId: String, segmentIndex: Int): Int? {
        return _messageSegmentHeights.value[messageId]?.get(segmentIndex)
    }
//    fun rememberMessageItemHeight(messageId: String, height: Int) {
//        _messageItemHeights.update { current ->
//            current + (messageId to height)
//        }
//    }
//
//    fun findMessageItemHeight(messageId: String): Int? {
//        return messageItemHeights.value[messageId]
//    }

    /**
     * 在后台线程加载背景图片的辅助函数
     */
    fun loadBackgroundBitmap(
        activity: ComponentActivity,
        backgroundPath: String,
        onResult: (Bitmap?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(backgroundPath)
                val bitmap: Bitmap? = if (file.exists()) {
                    val uri = Uri.fromFile(file)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val decodedBitmap = ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(activity.contentResolver, uri)
                        ) { decoder, info, source ->
                            // 禁用硬件加速，确保可以访问像素
                            decoder.setTargetColorSpace(
                                ColorSpace.get(ColorSpace.Named.SRGB)
                            )
                        }
                        // 如果是硬件Bitmap，转换为可修改的Bitmap
                        if (decodedBitmap.config == Bitmap.Config.HARDWARE) {
                            decodedBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        } else {
                            decodedBitmap
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
                    }
                } else {
                    Timber.tag("ChatActivity").e("背景图片文件不存在: $backgroundPath")
                    null
                }

                // 在主线程中执行回调
                withContext(Dispatchers.Main) {
                    onResult(bitmap)
                }
            } catch (e: Exception) {
                Timber.tag("ChatActivity").e(e, "加载背景图片失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult(null)
                }
            }
        }
    }
    private fun setupChatListener() {
        val listener =
            object : AIChatManager.AIChatMessageListener {
                override fun onMessageSent(message: ChatMessage) {
                    addMessage(message)
                }

                override fun onMessageReceived(message: ChatMessage) {
                    addMessage(message)
                }

                override fun onMessageChunk(messageId: String, chunk: String) {
                    viewModelScope.launch {
                        chatContext.replaceAll { message ->
                            if (message.id == messageId) {
                                message.copy(content = message.content + chunk)
                            } else {
                                message
                            }
                        }
                    }
                    // 实时更新消息内容
                    _messages.update { currentMessages ->
                        currentMessages.map { message ->
                            if (message.id == messageId) {
                                message.copy(content = message.content + chunk)
                            } else {
                                message
                            }
                        }
                    }
                }

                override fun onAllReplyCompleted() {
                    hideProgress()
                }

                override fun onError(error: String) {
//                    ToastUtils.showToast("ERROR")
                    hideProgress()
                    Timber.tag("Chat").e(error)
                }

                override fun onShowToast(message: String) {
                    ToastUtils.showToast(message)
                }
            }
        aiChatManager.registerListener(listener)
    }

    fun handleImageResult(
        activity: Activity,
        result: ActivityResult,
        onImageSelected: (Bitmap, Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(
                                activity.contentResolver,
                                imageUri
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(activity.contentResolver, imageUri)
                    }

                    // 保存图片到缓存
                    val file = saveImage(bitmap)
                    if (file != null) {
                        val savedUri = Uri.fromFile(file)
                        onImageSelected(bitmap, savedUri)
                        Toast.makeText(activity, "图片选择成功", Toast.LENGTH_SHORT).show()
                    } else {
                        onError("无法保存图片到缓存")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onError("图片加载失败: ${e.message}")
                    Toast.makeText(activity, "图片加载失败", Toast.LENGTH_SHORT).show()
                }
            } else {
                onError("未获取到图片URI")
            }
        }
    }

    // 添加辅助方法来创建图片选择Intent
    fun createImagePickerIntent(): Intent {
        return Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
    }

    fun saveImage(bitmap: Bitmap): File? =
        try {
            val imageManager = ImageManager()
            val conversationId = "${userConfigState.userConfig.value?.userId}_${getCurrentCharacter()?.id}"
            imageManager.saveChatImage(conversationId, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    fun refreshConversationInfo() {
        viewModelScope.launch {
            try {
                val updatedConversation = conversationRepository.getById(conversation.value.id)
                if (updatedConversation != null) {
                    _conversation.value = updatedConversation
                }
            } catch (e: Exception) {
                Timber.tag("BaseChatViewModel").e(e, "刷新对话信息失败")
            }
        }
    }


    private fun loadSupportedModels(baseUrl: String, apiKey: String) {
        ResultHandler.handleResultWithData(
            scope = viewModelScope,
            flow = aiHubRepository.getModels(baseUrl, apiKey).asResult(),
            onData = { modelsResponse ->
                _uiState.value = BaseNetWorkUiState.Success(modelsResponse)
                supportedModels = modelsResponse.data
                _chatUiState.update { it.copy(currentModelName = selectedModel) }
            },
            onError = { message, exception ->
                Timber.tag("ChatConfigViewModel").d("Error: $message Exception: $exception")
                // 更新 uiState 为错误状态
                _uiState.value = BaseNetWorkUiState.Error(message, exception)
                ToastUtils.showError("$message,$exception" )
            },
            onLoading = {
                // 更新 uiState 为加载状态
                _uiState.value = BaseNetWorkUiState.Loading
            }
        )
    }

    fun loadInitialData() {
        if (isInitialLoading) return
        isInitialLoading = true

        viewModelScope.launch {
            _chatUiState.update { it.copy(isLoading = true) }
            chatMessageRepository.getMessagesByPage(
                conversation.value.id,
                PAGE_SIZE,
                0
            ).also { messages ->
                _messages.value = messages
            }

            _chatUiState.update { it.copy(isLoading = false) }

            currentPage = 1
            val totalCount = chatMessageRepository.getTotalMessageCount(conversation.value.id)
            _chatUiState.update { it.copy(hasMoreData = totalCount > PAGE_SIZE) }

            isInitialLoading = false

            tempChatMessageRepository.getByConversationId(conversation.value.id)
                .takeLast(userConfigState.userConfig.value?.maxContextMessageSize ?: 15)
                .let {
                    chatContext.clear()
                    chatContext.addAll(it)
                }
            _chatUiState.update { it.copy(initFinished = true) }
        }
    }

    // 加载更多消息的方法
    fun loadMoreMessages() {
        if (_chatUiState.value.isLoading || !_chatUiState.value.hasMoreData || isInitialLoading) return
        Timber.tag("BaseChatViewModel").d("loadMoreMessages: $currentPage")

        viewModelScope.launch(Dispatchers.IO) {
            _chatUiState.update { it.copy(isLoading = true) }
            // 使用当前已加载的消息数量作为 offset，防止因新消息插入导致的 offset 偏移
            val offset = _messages.value.size
            val moreMessages = chatMessageRepository.getMessagesByPage(
                conversation.value.id,
                PAGE_SIZE,
                offset
            )

            withContext(Dispatchers.Main) {
                if (moreMessages.isNotEmpty()) {
                    _messages.update { currentMessages ->
                        val existingIds = currentMessages.map { it.id }.toSet()
                        // 过滤掉可能重复加载的消息
                        currentMessages + moreMessages.filter { it.id !in existingIds }
                    }
                    currentPage++

                    // 检查是否还有更多数据
                    val totalCount = chatMessageRepository.getTotalMessageCount(conversation.value.id)
                    _chatUiState.update {
                        it.copy(hasMoreData = _messages.value.size < totalCount)
                    }
                } else {
                    _chatUiState.update { it.copy(hasMoreData = false) }
                }
                _chatUiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // 添加检查是否可以加载更多消息的方法
    fun canLoadMore(): Boolean = _chatUiState.value.hasMoreData && !_chatUiState.value.isLoading

    // 添加获取加载状态的方法
    fun isLoading(): Boolean = _chatUiState.value.isLoading

    fun continueGenerate(isGroupChat: Boolean) = if (isGroupChat) sendGroupMessage() else sendMessage()

    fun reGenerate(isGroupChat: Boolean){
        // 检查上下文和消息列表
        if (chatContext.isEmpty() || _messages.value.isEmpty()) return
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
        // 移除消息
        _messages.update { currentMessages ->
            currentMessages.filter { !removeMessageIds.contains(it.id) }
        }
        // 移除数据库中的消息
        if (removeMessageIds.isNotEmpty()) {
            viewModelScope.launch {
                chatMessageRepository.deleteMessagesByIds(removeMessageIds)
                tempChatMessageRepository.deleteMessagesByIds(removeMessageIds)
            }
        }
    }
    fun sendMessage(
        messageText: String = "",
        isSendSystemMessage: Boolean = false,
        character: AICharacter? = currentAICharacter
    ) {
//        if (messageText.isEmpty()) return
        _chatUiState.update { it.copy(isAiReplying = true) }
        character?.let { character ->
            viewModelScope.launch {
                aiChatManager.sendMessage(
                    conversation = conversation.value,
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
        if (conversation.value.characterIds.isEmpty()) return
        _chatUiState.update { it.copy(isAiReplying = true) }
        viewModelScope.launch {
            lastGroupChatReply = aiChatManager.sendGroupMessage(
                conversation = conversation.value,
                aiCharacters = _chatUiState.value.currentCharacters,
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
        _chatUiState.update { it.copy(isAiReplying = true) }
        character?.let { character ->
            val tempChatContext = chatContext
            viewModelScope.launch {
                aiChatManager.sendImage(
                    conversation = conversation.value,
                    character,
                    bitmap,
                    savedUri,
                    tempChatContext
                )
            }
        }
    }

    fun addMessage(message: ChatMessage) {
        if (message.conversationId != conversation.value.id) return
        _messages.update { currentMessages ->
            // 防止重复添加具有相同 ID 的消息导致 LazyColumn Key 重复崩溃
            if (currentMessages.any { it.id == message.id }) {
                currentMessages
            } else {
                listOf(message) + currentMessages
            }
        }

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
                conversationId = conversation.value.id
            )
            chatContext.add(tempMessage)
        }
        _chatUiState.update { it.copy(receiveNewMessage = true) }
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageRepository.deleteMessagesByConversationId(conversation.value.id)
            tempChatMessageRepository.deleteByConversationId(conversation.value.id)

            withContext(Dispatchers.Main) {
                _messages.value = emptyList()
                chatContext.clear()
            }
        }
    }

    fun selectModel(modelId: String) {
        selectedModel = modelId
        viewModelScope.launch {
            userConfigState.userConfig.value?.let {
                userConfigState.updateUserConfig(it.copy(selectedModel = modelId))
            }
        }
        _chatUiState.update { it.copy(currentModelName = selectedModel) }
    }

    fun getSupportedModels(): List<Model> = supportedModels

    fun getCurrentCharacter(): AICharacter? = currentAICharacter
    fun hideProgress() {
        _chatUiState.update { it.copy(isAiReplying = false) }
    }
    suspend fun deleteConversation(conversation: Conversation): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // 先删除对话的所有消息
                chatMessageRepository.deleteMessagesByConversationId(conversation.id)
                tempChatMessageRepository.deleteByConversationId(conversation.id)

                // 删除对话所有相关图片
                val tag1 = imageManager.deleteAllChatImages(conversation.id)
                val tag2 = aiChatMemoryRepository.deleteByConversationId(conversation.id) >= 0
                // 最后删除对话
                val tag3 = conversationRepository.deleteById(conversation.id) > 0

                Timber.tag("BaseChatViewModel").e("删除对话: $tag1 $tag2 $tag3")
                tag1 && tag2 && tag3
            }
        } catch (e: Exception) {
             Timber.tag("BaseChatViewModel").e(e, "删除对话失败: ${e.message}")
            false
        }
    }

    fun setReceiveNewMessage(receiveNewMessage: Boolean) {
        _chatUiState.update { it.copy(receiveNewMessage = receiveNewMessage) }
    }

    fun updateMessageContent(messageId: String, content: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                // 更新数据库
                chatMessageRepository.updateMessageContent(messageId, content)
                tempChatMessageRepository.updateMessageContent(messageId, content)

                // 更新消息列表
                _messages.update { currentMessages ->
                    currentMessages.map { message ->
                        if (message.id == messageId) message.copy(content = content) else message
                    }
                }
                chatContext.forEach { message ->
                    if (message.id == messageId) {
                        message.content = content
                    }
                }

                onComplete?.invoke(true)
            } catch (e: Exception) {
                Timber.tag("BaseChatViewModel").e(e, "修改消息内容失败: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    fun deleteOneMessage(messageId: String) {
        viewModelScope.launch {
            try {
                // 删除数据库中的消息
                chatMessageRepository.deleteMessageById(messageId)
                tempChatMessageRepository.deleteMessageById(messageId)

                // 更新消息列表
                _messages.update { currentMessages ->
                    currentMessages.filter { it.id != messageId }
                }
                
                chatContext.removeIf { it.id == messageId }

                Timber.tag("BaseChatViewModel").e("删除消息: $messageId")
            } catch (e: Exception) {
                Timber.tag("BaseChatViewModel").e(e, "删除消息失败: ${e.message}")
            }
        }
    }

    fun paseMessage(message: ChatMessage): ChatUtils.ParsedMessage {
        return chatUtils.parseMessage(message)
    }

    fun paseMessage(message: TempChatMessage): ChatUtils.ParsedMessage {
        return chatUtils.parseMessage(message)
    }

    fun regexReplace(content: String): String {
        return chatUtils.regexReplace(content, regexList.value)
    }
}

data class ChatUiState(
    val currentCharacter: AICharacter? = null,
    val currentCharacters: List<AICharacter> = emptyList(),
    val currentModelName: String = "",
    val isLoading: Boolean = false,
    val isAiReplying: Boolean = false,
    val initFinished: Boolean = false,
    val hasMoreData: Boolean = true,
    val receiveNewMessage: Boolean = false
)
