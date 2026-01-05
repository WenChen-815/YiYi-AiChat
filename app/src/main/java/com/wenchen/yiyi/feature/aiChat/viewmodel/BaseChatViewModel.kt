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
import com.wenchen.yiyi.core.database.entity.ChatMessage
import com.wenchen.yiyi.core.database.entity.TempChatMessage
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.feature.aiChat.common.AIChatManager
import com.wenchen.yiyi.core.common.ApiService
import com.wenchen.yiyi.core.datastore.storage.ImageManager
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.core.database.entity.Model
import com.wenchen.yiyi.core.util.LimitMutableList
import com.wenchen.yiyi.core.util.limitMutableListOf
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.util.toast.ToastUtils
import com.wenchen.yiyi.navigation.AppNavigator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.collections.toMutableList

abstract class BaseChatViewModel(
    navigator: AppNavigator,
    userState: UserState,
    val userConfigState: UserConfigState,
    savedStateHandle: SavedStateHandle
): BaseViewModel(
    navigator = navigator,
    userState = userState
) {
    val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    var currentAICharacter: AICharacter? = null

    val imageManager = ImageManager()
    private var apiService: ApiService? = null
    private var selectedModel: String = ""
    private var supportedModels: List<Model> = emptyList()

    private var isInitialLoading = false

    val chatMessageDao = Application.appDatabase.chatMessageDao()
    val tempChatMessageDao = Application.appDatabase.tempChatMessageDao()
    val aiCharacterDao = Application.appDatabase.aiCharacterDao()
    val aiChatMemoryDao = Application.appDatabase.aiChatMemoryDao()
    val conversationDao = Application.appDatabase.conversationDao()

    private val PAGE_SIZE = 15
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

    init {
        userConfigState.userConfig.onEach { 
            initApiService()
        }.launchIn(viewModelScope)
    }

    fun init() {
        initApiService()
        setupChatListener()
    }

    private fun initApiService() {
        val userConfig = userConfigState.userConfig.value
        val apiKey = userConfig?.baseApiKey ?: ""
        val baseUrl = userConfig?.baseUrl ?: ""
        selectedModel = userConfig?.selectedModel ?: ""
        apiService = ApiService(baseUrl, apiKey)
        loadSupportedModels()
    }
    /**
     * 在后台线程加载背景图片的辅助函数
     */
    suspend fun loadBackgroundBitmap(activity: ComponentActivity, backgroundPath: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(backgroundPath)
                if (file.exists()) {
                    val uri = Uri.fromFile(file)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val bitmap = ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(activity.contentResolver, uri)
                        ) { decoder, info, source ->
                            // 禁用硬件加速，确保可以访问像素
                            decoder.setTargetColorSpace(
                                ColorSpace.get(ColorSpace.Named.SRGB)
                            )
                        }
                        // 如果是硬件Bitmap，转换为可修改的Bitmap
                        if (bitmap.config == Bitmap.Config.HARDWARE) {
                            bitmap.copy(Bitmap.Config.ARGB_8888, false)
                        } else {
                            bitmap
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
                    }
                } else {
                    Timber.tag("ChatActivity").e("背景图片文件不存在: $backgroundPath")
                    null
                }
            } catch (e: Exception) {
                Timber.tag("ChatActivity").e(e, "加载背景图片失败: ${e.message}")
                null
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
                    // 隐藏进度条
                    hideProgress()
                }

                override fun onAllReplyCompleted() {}

                override fun onError(error: String) {
                    ToastUtils.showToast(error)
                }

                override fun onShowToast(message: String) {
                    ToastUtils.showToast(message)
                }
            }
        AIChatManager.registerListener(listener)
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
            val conversationId = "${userConfigState.userConfig.value?.userId}_${getCurrentCharacter()?.aiCharacterId}"
            imageManager.saveChatImage(conversationId, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    fun refreshConversationInfo() {
        viewModelScope.launch {
            try {
                val updatedConversation = conversationDao.getById(conversation.value.id)
                if (updatedConversation != null) {
                    _conversation.value = updatedConversation
                }
            } catch (e: Exception) {
                Timber.tag("BaseChatViewModel").e(e, "刷新对话信息失败")
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

    fun loadInitialData() {
        if (isInitialLoading) return
        isInitialLoading = true

        Timber.tag("BaseChatViewModel").d("loadInitialData: ${conversation.value.id}")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val initialMessages = chatMessageDao.getMessagesByPage(
                conversation.value.id,
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

            tempChatMessageDao.getByConversationId(conversation.value.id)
                .takeLast(userConfigState.userConfig.value?.maxContextMessageSize ?: 15)
                .let {
                    chatContext.clear()
                    chatContext.addAll(it)
                }
        }
    }

    // 加载更多消息的方法
    fun loadMoreMessages() {
        if (_uiState.value.isLoading || !_uiState.value.hasMoreData || isInitialLoading) return
        Timber.tag("BaseChatViewModel").d("loadMoreMessages: $currentPage")

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val offset = currentPage * PAGE_SIZE
            val moreMessages = chatMessageDao.getMessagesByPage(
                conversation.value.id,
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
        _uiState.value = _uiState.value.copy(isAiReplying = true)
        viewModelScope.launch {
            lastGroupChatReply = AIChatManager.sendGroupMessage(
                conversation = conversation.value,
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
                conversationId = conversation.value.id
            )
            chatContext.add(tempMessage)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.deleteMessagesByConversationId(conversation.value.id)
            tempChatMessageDao.deleteByConversationId(conversation.value.id)

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
        viewModelScope.launch {
            userConfigState.userConfig.value?.let {
                userConfigState.updateUserConfig(it.copy(selectedModel = modelId))
            }
        }
        _uiState.value = _uiState.value.copy(
            currentModelName = selectedModel
        )
    }

    fun getSupportedModels(): List<Model> = supportedModels

    fun getCurrentCharacter(): AICharacter? = currentAICharacter
    fun hideProgress() {
        _uiState.value = _uiState.value.copy(isAiReplying = false)
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

                Timber.tag("BaseChatViewModel").e("删除对话: $tag1 $tag2 $tag3")
                tag1 && tag2 && tag3
            }
        } catch (e: Exception) {
             Timber.tag("BaseChatViewModel").e(e, "删除对话失败: ${e.message}")
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
                Timber.tag("BaseChatViewModel").e(e, "修改消息内容失败: ${e.message}")
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

                Timber.tag("BaseChatViewModel").e("删除消息: $messageId")
            } catch (e: Exception) {
                Timber.tag("BaseChatViewModel").e(e, "删除消息失败: ${e.message}")
            }
        }
    }
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentCharacter: AICharacter? = null,
    val currentCharacters: List<AICharacter> = emptyList(),
    val currentModelName: String = "",
    val isLoading: Boolean = false,
    val isAiReplying: Boolean = false,
    val hasMoreData: Boolean = true,
    val receiveNewMessage: Boolean = false
)
