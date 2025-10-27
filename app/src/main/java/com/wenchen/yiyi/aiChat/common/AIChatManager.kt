package com.wenchen.yiyi.aiChat.common

import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.wenchen.yiyi.aiChat.entity.ChatMessage
import com.wenchen.yiyi.aiChat.entity.Conversation
import com.wenchen.yiyi.aiChat.entity.MessageContentType
import com.wenchen.yiyi.aiChat.entity.MessageType
import com.wenchen.yiyi.aiChat.entity.TempChatMessage
import com.wenchen.yiyi.common.App
import com.wenchen.yiyi.common.entity.AICharacter
import com.wenchen.yiyi.aiChat.entity.AIChatMemory
import com.wenchen.yiyi.common.entity.Message
import com.wenchen.yiyi.common.utils.ChatUtil
import com.wenchen.yiyi.config.common.ConfigManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object AIChatManager {
    // 基础配置
    private const val TAG = "AIChatManager"
    private lateinit var configManager: ConfigManager
    private lateinit var apiKey: String
    private lateinit var baseUrl: String
    private lateinit var apiService: ApiService

    // 图片识别配置
    private var imgApiKey: String? = null
    private var imgBaseUrl: String? = null
    private var selectedImgModel: String? = null

    // 聊天参数配置
    private var maxContextMessageSize: Int = 10
    private var summarizeTriggerCount: Int = 20

    // 数据访问对象
    private val chatMessageDao = App.Companion.appDatabase.chatMessageDao()
    private val tempChatMessageDao = App.Companion.appDatabase.tempChatMessageDao()
    private val aiChatMemoryDao = App.Companion.appDatabase.aiChatMemoryDao()

    // 并发控制
    private val characterLocks = ConcurrentHashMap<String, Mutex>()
    private val listeners = Collections.synchronizedSet(mutableSetOf<AIChatMessageListener>())

    // 用户信息
    private var USER_ID = "21107"
    private var USER_NAME = "用户20815"

    fun init() {
        // 基础配置初始化
        configManager = ConfigManager()
        apiKey = configManager.getApiKey() ?: ""
        baseUrl = configManager.getBaseUrl() ?: ""

        // 图片识别配置初始化
        imgApiKey = configManager.getImgApiKey()
        imgBaseUrl = configManager.getImgBaseUrl()
        selectedImgModel = configManager.getSelectedImgModel()
        apiService = ApiService(baseUrl, apiKey, imgBaseUrl, imgApiKey)

        // 用户信息初始化
        USER_ID = configManager.getUserId().toString()
        USER_NAME = configManager.getUserName().toString()

        // 聊天参数配置初始化
        maxContextMessageSize = configManager.getMaxContextMessageSize()
        summarizeTriggerCount = configManager.getSummarizeTriggerCount()
    }


    // 注册监听器
    fun registerListener(listener: AIChatMessageListener) {
        listeners.add(listener)
    }

    // 取消注册监听器
    fun unregisterListener(listener: AIChatMessageListener) {
        listeners.remove(listener)
    }

    /**
     * 发送消息并与AI进行交互的核心函数。该函数会构建完整的对话上下文（包括系统提示、历史记录和当前用户输入），
     * 并调用模型接口完成响应生成与记忆更新。
     *
     * @param conversation 当前聊天会话对象，包含玩家信息及场景描述等上下文数据。
     * @param aiCharacter 当前使用的AI角色配置信息，如身份设定、外观、行为规则等。
     * @param newMessageTexts 新增的用户消息文本列表，将依次作为用户发言加入到对话中。
     * @param oldMessages 历史临时消息列表，用于构造上下文中的过往对话内容。
     * @param showInChat 是否在界面上显示这些新发送的消息，默认为true。
     * @param isSendSystemMessage 标识是否以系统旁白形式发送消息，默认为false。
     */
    suspend fun sendMessage(
        conversation: Conversation,
        aiCharacter: AICharacter?,
        newMessageTexts: List<String>,
        oldMessages: List<TempChatMessage>,
        showInChat: Boolean = true,
        isSendSystemMessage: Boolean = false
    ) {
        // 参数校验
        if (aiCharacter == null) {
            Log.e(TAG, "未选择AI角色")
            return
        }
        // 构建消息列表
        val messages = mutableListOf<Message>()

        // 添加系统提示消息
        val prompt = buildString {
            if (aiCharacter.roleIdentity.isNotBlank()) {
                append("# 角色任务&身份\n${aiCharacter.roleIdentity}\n")
            }
            if (aiCharacter.roleAppearance.isNotBlank()) {
                append("# 角色外貌\n${aiCharacter.roleAppearance}\n")
            }
            if (aiCharacter.roleDescription.isNotBlank()) {
                append("# 角色描述\n${aiCharacter.roleDescription}\n")
            }
            append("# 用户简介:${conversation.playerName} ")
            if (conversation.playGender.isNotBlank()) {
                append("性别:${conversation.playGender} ")
            }
            if (conversation.playerDescription.isNotBlank()) {
                append("描述:${conversation.playerDescription}")
            }
            if (conversation.chatSceneDescription.isNotBlank()) {
                append("\n# [[当前场景]]: ${conversation.chatSceneDescription}\n")
            }
            if (aiCharacter.outputExample.isNotBlank()) {
                append("# 角色输出示例\n${aiCharacter.outputExample}\n")
            }
            if (aiCharacter.behaviorRules.isNotBlank()) {
                append("# [[以下行为准则请严格遵守]]\n${aiCharacter.behaviorRules}\n")
            }
        }.trim()
        Log.d(TAG, "prompt: $prompt")

        val history = aiChatMemoryDao.getByCharacterIdAndConversationId(
            aiCharacter.aiCharacterId,
            conversation.id
        )?.content

        if (prompt.isNotEmpty()) {
            messages.add(Message("system", "$prompt\n# 以下为角色记忆:\n$history"))
        }

        // 添加历史消息
        for (message in oldMessages) {
            if (message.type == MessageType.ASSISTANT) {
                messages.add(Message(message.type.name.lowercase(), ChatUtil.parseMessage(message)))
            } else {
                messages.add(Message(message.type.name.lowercase(), message.content))
            }
        }

        // 处理新消息
        val currentDate =
            SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
        val currentUserName = "[${conversation.playerName}]"
        for (newMessageText in newMessageTexts) {
            var newContent = if (isSendSystemMessage) {
                "${currentDate}|[系统旁白] $newMessageText"
            } else {
                "${currentDate}|${currentUserName} $newMessageText"
            }
            val userMessage = ChatMessage(
                id = "${aiCharacter.aiCharacterId}:${System.currentTimeMillis()}",
                content = newContent,
                type = if (isSendSystemMessage) MessageType.SYSTEM else MessageType.USER,
                characterId = aiCharacter.aiCharacterId,
                chatUserId = USER_ID,
                isShow = showInChat,
                conversationId = conversation.id
            )
            // 通知所有监听器消息已发送
            CoroutineScope(Dispatchers.Main).launch {
                listeners.forEach { it.onMessageSent(userMessage) }
            }
            // 添加新消息到待发送列表
            messages.add(Message(if (isSendSystemMessage) "system" else "user", newContent))
            // 保存用户消息到数据库
            chatMessageDao.insertMessage(userMessage)
            tempChatMessageDao.insert(
                TempChatMessage(
                    id = userMessage.id,
                    content = userMessage.content,
                    type = userMessage.type,
                    characterId = userMessage.characterId,
                    chatUserId = userMessage.chatUserId,
                    isShow = userMessage.isShow,
                    conversationId = userMessage.conversationId
                )
            )
        }

        // 发送消息和总结
        Log.i(TAG, "用户 调用总结")
        send(conversation, aiCharacter, messages)
        summarize(conversation, aiCharacter)
    }

    /**
     * 发送消息给AI模型并处理响应的核心方法
     *
     * @param conversation 当前聊天会话对象，包含会话相关信息
     * @param aiCharacter AI角色对象，包含角色身份、描述等配置信息
     * @param messages 要发送的消息列表，包括系统提示、历史消息和用户最新消息
     */
    private fun send(
        conversation: Conversation,
        aiCharacter: AICharacter?,
        messages: MutableList<Message>
    ) {
        if (aiCharacter == null) {
            Log.e(TAG, "未选择AI角色")
            return
        }
        apiService.sendMessage(
            messages = messages,
            model = configManager.getSelectedModel() ?: "",
            temperature = 1.1f,
            onSuccess = { aiResponse ->
                // 创建时间戳和格式化日期
                val timestamp = System.currentTimeMillis()
                val currentDate =
                    SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
                val currentCharacterName = "[${aiCharacter.name}]"
                val messageId = "${aiCharacter.aiCharacterId}:$timestamp"
                val messageContent = "$currentDate|${currentCharacterName} $aiResponse"

                val aiMessage = ChatMessage(
                    id = messageId,
                    content = messageContent,
                    type = MessageType.ASSISTANT,
                    characterId = aiCharacter.aiCharacterId,
                    chatUserId = USER_ID,
                    conversationId = conversation.id
                )
                // 保存AI消息到数据库 创建一个新的协程来执行挂起函数
                CoroutineScope(Dispatchers.IO).launch {
                    chatMessageDao.insertMessage(aiMessage)
                    tempChatMessageDao.insert(
                        TempChatMessage(
                            id = messageId,
                            content = messageContent,
                            type = MessageType.ASSISTANT,
                            characterId = aiCharacter.aiCharacterId,
                            chatUserId = USER_ID,
                            conversationId = conversation.id
                        )
                    )
                    Log.i(TAG, "AI调用总结")
                    summarize(conversation, aiCharacter)
                }
                Log.d(TAG, "AI回复:${aiMessage.content}")
                CoroutineScope(Dispatchers.Main).launch {
                    listeners.forEach { it.onMessageReceived(aiMessage) }
                }
            },
            onError = { errorMessage ->
                CoroutineScope(Dispatchers.Main).launch {
                    listeners.forEach { it.onError(errorMessage) }
                }
            }
        )
    }

    /**
     * 对指定角色与用户的对话进行总结，并将总结结果保存为记忆片段。
     *
     * @param conversation 当前对话对象，用于标识对话上下文。
     * @param aiCharacter 参与对话的AI角色信息。若为空则不执行操作。
     * @param callback 操作完成后的回调函数（可选）。
     */
    suspend fun summarize(
        conversation: Conversation,
        aiCharacter: AICharacter?,
        callback: (() -> Unit)? = null
    ) {
        // 添加用户消息到列表
        if (aiCharacter == null) {
            Log.e(TAG, "summarize: 未选择AI角色")
            return
        }

        val characterId = aiCharacter.aiCharacterId
        // 获取该角色对应的锁，如果不存在则创建一个新的锁
        val characterLock = characterLocks.computeIfAbsent(characterId) { Mutex() }
        // 尝试获取角色锁，如果已被锁定则等待
        characterLock.withLock {
            var aiMemory = aiChatMemoryDao.getByCharacterIdAndConversationId(
                aiCharacter.aiCharacterId,
                conversation.id
            )
            aiMemory?.count?.let {
                if (it >= configManager.getMaxSummarizeCount()) {
                    Log.d(TAG, "已达到最大总结次数")
                    CoroutineScope(Dispatchers.Main).launch {
                        listeners.forEach { listener ->
                            listener.onShowToast("已达到最大总结次数")
                        }
                    }
                    return@withLock
                }
            }
            val count = tempChatMessageDao.getCountByCharacterId(aiCharacter.aiCharacterId)
            if (count < summarizeTriggerCount) {
                Log.d(TAG, "无需总结 count=$count")
                return@withLock
            }
            Log.d(TAG, "开始总结 count=$count")
            val allHistory = tempChatMessageDao.getByCharacterId(aiCharacter.aiCharacterId)
            val summaryMessages = allHistory.subList(0, allHistory.size - maxContextMessageSize)
            // 构建消息列表
            val messages = mutableListOf<Message>()
            // 添加系统提示消息
            val prompt = buildString {
                if (aiCharacter.roleIdentity.isNotBlank()) {
                    append("## 角色身份\n${aiCharacter.roleIdentity}\n")
                }
                if (aiCharacter.roleDescription.isNotBlank()) {
                    append("## 角色描述\n${aiCharacter.roleDescription}\n")
                }
            }.trim()
            val currentDate =
                SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
            var summaryRequest =
                """
                # [任务] 根据对话内容、角色设定、当前时间，使用中文总结对话中的重要信息
                # [当前时间] [$currentDate]
                # [角色设定]
                $prompt
                
                现在请你代入${aiCharacter.name}的角色并以“我”自称，用中文总结与${conversation.playerName}的对话，结合代入角色的性格特点，将以下对话片段提取重要信息总结为一段话作为记忆片段(不要有总结内容以外的说明，直接回复一段话):
            """.trimIndent()
            // 添加历史消息
            for (message in summaryMessages) {
                summaryRequest += "\n${message.content}"
            }
            messages.add(Message("system", summaryRequest))
            // 使用CompletableDeferred来等待API调用完成
            val apiCompleted = CompletableDeferred<Boolean>()
            apiService.sendMessage(
                messages = messages,
                model = configManager.getSelectedModel() ?: "",
                temperature = 0.3f,
                onSuccess = { aiResponse ->
                    // 收到回复
                    var newMemoryContent =
                        """
                        ## 记忆片段 [${currentDate}]
                        **摘要**:$aiResponse
                    """.trimIndent()
                    CoroutineScope(Dispatchers.IO).launch {
                        Log.d(TAG, "总结成功 delete :${summaryMessages.map { it.content }}")
                        // 删除已总结的临时消息
                        tempChatMessageDao.deleteAll(summaryMessages)
                        Log.d(
                            TAG,
                            "characterId:$characterId conversationId:${conversation.id} aiMemory :$aiMemory"
                        )
                        if (aiMemory == null) {
                            aiMemory = AIChatMemory(
                                id = UUID.randomUUID().toString(),
                                characterId = aiCharacter.aiCharacterId,
                                conversationId = conversation.id,
                                content = newMemoryContent,
                                createdAt = System.currentTimeMillis()
                            )
                            val result = aiChatMemoryDao.insert(aiMemory)
                            Log.d(TAG, "总结成功 insert :$result")
                        } else {
                            aiMemory.content =
                                if (aiMemory.content.isNotEmpty() == true) "${aiMemory.content}\n\n$newMemoryContent" else newMemoryContent
                            val result = aiChatMemoryDao.update(aiMemory)
                            Log.d(TAG, "总结成功 update :$result")
                        }
                        callback?.invoke()
                        apiCompleted.complete(true)
                    }
                },
                onError = {
                    // 发生错误
                    Log.e(TAG, "summarize: 总结失败")
                    apiCompleted.complete(false)
                }
            )
            // 等待API调用完成后再释放锁
            apiCompleted.await()
        }
    }

    /**
     * 发送图片消息并调用图片识别接口获取图片描述，再将描述作为用户输入发送给AI角色。
     *
     * @param conversation 当前对话信息对象，用于标识聊天上下文。
     * @param character 当前AI角色信息，用于构建消息和发送逻辑。
     * @param bitmap 图片的Bitmap格式数据，用于转为Base64后上传至图片识别服务。
     * @param imgUri 图片的Uri路径，主要用于记录与展示。
     * @param oldMessages 历史临时消息列表，用于保持会话上下文。
     */
    suspend fun sendImage(
        conversation: Conversation,
        character: AICharacter,
        bitmap: Bitmap,
        imgUri: Uri,
        oldMessages: List<TempChatMessage>
    ) {
        // 检查图片识别是否启用
        if (!configManager.isImgRecognitionEnabled()) {
            Log.e(TAG, "图片识别功能未启用")
            CoroutineScope(Dispatchers.Main).launch {
                listeners.forEach { it.onError("图片识别功能未启用") }
            }
            return
        }

        // 检查配置是否完整
        if (imgApiKey.isNullOrEmpty() || imgBaseUrl.isNullOrEmpty() || selectedImgModel.isNullOrEmpty()) {
            Log.e(TAG, "图片识别配置不完整")
            CoroutineScope(Dispatchers.Main).launch {
                listeners.forEach { it.onError("图片识别配置不完整") }
            }
            return
        }

        val imgMessage = ChatMessage(
            id = "${character.aiCharacterId}:${System.currentTimeMillis()}",
            content = "发送了图片:[$imgUri]",
            type = MessageType.USER,
            characterId = character.aiCharacterId,
            chatUserId = USER_ID,
            contentType = MessageContentType.IMAGE,
            imgUrl = imgUri.toString(),
            conversationId = conversation.id
        )
        // 通知所有监听器消息已发送
        CoroutineScope(Dispatchers.Main).launch {
            // 通知所有监听器消息已接收
            listeners.forEach { it.onMessageSent(imgMessage) }
        }
        chatMessageDao.insertMessage(imgMessage)
        // 将Bitmap转换为Base64字符串
        val imageBase64 = bitmapToBase64(bitmap)
        // 准备提示文本
        val prompt =
            "请用中文描述这张图片的主要内容或主题。不要使用'这是'、'这张'等开头，直接描述。如果有文字，请包含在描述中。"
        // 调用API发送图片识别请求
        apiService.sendMultimodalMessage(
            prompt = prompt,
            imageBase64 = imageBase64,
            model = selectedImgModel!!,
            onSuccess = { imgDescription ->
                Log.d(TAG, "图片识别成功，回复：$imgDescription")
                CoroutineScope(Dispatchers.IO).launch {
                    val desc = "[${conversation.playerName}]向你发送了图片:[$imgDescription]"
                    sendMessage(conversation, character, listOf(desc), oldMessages, false)
                }
            },
            onError = { errorMessage ->
                Log.e(TAG, "图片识别失败：$errorMessage")
                CoroutineScope(Dispatchers.Main).launch {
                    listeners.forEach { it.onError("图片识别失败：$errorMessage") }
                }
            }
        )

    }

    // 将Bitmap转换为Base64字符串
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos) // 压缩图片质量为100%
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    interface AIChatMessageListener {
        fun onMessageSent(message: ChatMessage)
        fun onMessageReceived(message: ChatMessage)
        fun onError(error: String)
        fun onShowToast(message: String)
    }
}