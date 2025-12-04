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
import com.wenchen.yiyi.aiChat.entity.ConversationType
import com.wenchen.yiyi.common.ApiService
import com.wenchen.yiyi.common.entity.Message
import com.wenchen.yiyi.common.utils.BitMapUtil
import com.wenchen.yiyi.common.utils.ChatUtil
import com.wenchen.yiyi.config.common.ConfigManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val chatMessageDao = App.appDatabase.chatMessageDao()
    private val tempChatMessageDao = App.appDatabase.tempChatMessageDao()
    private val aiChatMemoryDao = App.appDatabase.aiChatMemoryDao()

    // 并发控制
    private val characterLocks = ConcurrentHashMap<String, Mutex>()
    private val sendCharacterQueue = ConcurrentHashMap<String, MutableList<AICharacter>>()
    private val isProcessing = ConcurrentHashMap<String, Boolean>()
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

    suspend fun sendGroupMessage(
        conversation: Conversation,
        aiCharacters: List<AICharacter>,
        newMessageTexts: List<String>,
        oldMessages: List<TempChatMessage>,
        showInChat: Boolean = true,
        isSendSystemMessage: Boolean = false
    ) {
        // 参数校验
        if (conversation.characterIds.isEmpty() || aiCharacters.isEmpty()) {
            Log.e(TAG, "未选择AI角色")
            return
        }
        /*
         * 此处冗余，但予以保留
         * 这里不需要用到userMessage的原因是在handleUserMessage方法中将处理后的消息插入了数据库并通知给了ChatActivity，
         * 而ChatActivity的viewmodel会更新上下文，并且传递的参数本来是就是上下文的引用，所以上下文是最新的，不需要重复添加
        */
        val userMessage =
            handleUserMessage(conversation, newMessageTexts, isSendSystemMessage, showInChat)
        val count = tempChatMessageDao.getCountByConversationId(conversation.id)
        Log.d(TAG, "sendGroupMessage: $count")
        val summaryMessages = getSummaryMessages(conversation)
        if (count >= summarizeTriggerCount) aiCharacters.forEach { aiCharacter ->
            summarize(conversation, aiCharacter, summaryMessages)
        }
        // 初始化队列和状态
        val queueKey = conversation.id
        if (!sendCharacterQueue.containsKey(queueKey)) {
            sendCharacterQueue[queueKey] = mutableListOf()
            isProcessing[queueKey] = false
        }

        conversation.characterIds.forEach { (id, chance) ->
            val aiCharacter = aiCharacters.find { it.aiCharacterId == id }
            if (aiCharacter == null) {
                Log.e(TAG, "未找到AI角色: $id")
                return
            }
            val random = Math.random()
//            Log.d(TAG, "sendGroupMessage: $id -- $chance -- $random")
            if (random < chance) { // 生成一个0~1的随机数
                sendCharacterQueue[queueKey]?.add(aiCharacter)
            }
        }

        // 启动处理流程
        processMessageQueue(conversation, oldMessages)
    }

    private suspend fun processMessageQueue(
        conversation: Conversation,
        oldMessages: List<TempChatMessage>
    ) {
        val queueKey = conversation.id
        if (isProcessing[queueKey] == true) return

        isProcessing[queueKey] = true
        sendCharacterQueue[queueKey]?.shuffle() // 对发送队列进行乱序处理
        while (sendCharacterQueue[queueKey]?.isNotEmpty() == true) {
            val aiCharacter = sendCharacterQueue[queueKey]?.removeAt(0) ?: break
            Log.d(TAG, "oldMessages: $oldMessages")
            val messages = generateBaseMessages(aiCharacter, conversation, oldMessages)
            send(conversation, aiCharacter, messages)

            // 添加随机时间间隔
            val randomDelay = (2000 + (Math.random() * 4000)).toLong() // 2-6秒随机延迟
            delay(randomDelay)
        }
        clearMessageQueue(queueKey)
    }

    fun clearMessageQueue(conversationId: String) {
        sendCharacterQueue.remove(conversationId)
        isProcessing.remove(conversationId)
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
        // 处理新消息
        val userMessages =
            handleUserMessage(conversation, newMessageTexts, isSendSystemMessage, showInChat)
        val messages = generateBaseMessages(aiCharacter, conversation, oldMessages)

        // 发送消息和总结
//        messages.addAll(userMessages)
        send(conversation, aiCharacter, messages)

        val count = tempChatMessageDao.getCountByConversationId(conversation.id)
        if (count >= summarizeTriggerCount) {
            val summaryMessages = getSummaryMessages(conversation)
            Log.d(TAG, "开始总结 count=$count")
            summarize(conversation, aiCharacter, summaryMessages)
        }
    }

    private suspend fun generateBaseMessages(
        aiCharacter: AICharacter,
        conversation: Conversation,
        oldMessages: List<TempChatMessage>
    ): MutableList<Message> {
        val messages = mutableListOf<Message>()
        // 添加系统提示消息
        val prompt = buildString {
            if (conversation.type == ConversationType.GROUP) {
                append("**你正处于多人对话中，你的主要服务对象是[${conversation.playerName}]，请使用以下[${aiCharacter.name}]的身份参与对话**\n")
            }
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
            append("""# [[以下行为准则请严格遵守]]
                    [PRIORITY 1]收到系统旁白消息时，必须根据其中提示内容进行扩写
                    [PRIORITY 2]在“。 ？ ！ …”等表示句子结束处，或根于语境需要分隔处使用反斜线 (\) 分隔,以确保良好的可读性，但严格要求[]中的内容不允许使用(\)来分隔
                    ## 其他规则""")
            if (aiCharacter.behaviorRules.isNotBlank()) {
                append(aiCharacter.behaviorRules)
            }
        }.trim()
//        Log.d(TAG, "prompt: $prompt")

        val history = aiChatMemoryDao.getByCharacterIdAndConversationId(
            aiCharacter.aiCharacterId,
            conversation.id
        )?.content

        if (prompt.isNotEmpty()) {
            messages.add(Message("system", "$prompt\n# 以下为角色记忆:\n$history"))
        }
        // 添加历史消息
        for (message in oldMessages) {
            if (message.type == MessageType.ASSISTANT && message.characterId == aiCharacter.aiCharacterId) {
                messages.add(Message("assistant", ChatUtil.parseMessage(message)))
            } else {
                messages.add(Message("user", message.content))
            }
        }
        return messages
    }

    private suspend fun savaMessage(message: ChatMessage, saveToTemp: Boolean) {
        chatMessageDao.insertMessage(message)
        if (saveToTemp) {
            tempChatMessageDao.insert(
                TempChatMessage(
                    id = message.id,
                    content = message.content,
                    type = message.type,
                    characterId = message.characterId,
                    chatUserId = message.chatUserId,
                    isShow = message.isShow,
                    conversationId = message.conversationId
                )
            )
        }
    }

    private suspend fun handleUserMessage(
        conversation: Conversation,
        newMessageTexts: List<String>,
        isSendSystemMessage: Boolean,
        showInChat: Boolean
    ): MutableList<Message> {
        val userMessages = mutableListOf<Message>()
        val currentDate =
            SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
        val currentUserName = "[${conversation.playerName}]"
        for (newMessageText in newMessageTexts) {
            val newContent = if (isSendSystemMessage) {
                "${currentDate}|[系统旁白] $newMessageText"
            } else {
                "${currentDate}|${currentUserName} $newMessageText"
            }
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = newContent,
                type = if (isSendSystemMessage) MessageType.SYSTEM else MessageType.USER,
                characterId = "user",
                chatUserId = USER_ID,
                isShow = showInChat,
                conversationId = conversation.id
            )
            // 保存用户消息到数据库
            savaMessage(userMessage, true)
            // 添加新消息到待发送列表
            userMessages.add(Message(if (isSendSystemMessage) "system" else "user", newContent))
            // 通知所有监听器消息已发送
            CoroutineScope(Dispatchers.Main).launch {
                listeners.forEach { it.onMessageSent(userMessage) }
            }
        }
        return userMessages
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
        messages: MutableList<Message>,
    ) {
        if (aiCharacter == null) {
            Log.e(TAG, "未选择AI角色")
            return
        }
        Log.d(TAG, "send to ${aiCharacter.name}: $messages")
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
                val messageContent = "$currentDate|${currentCharacterName} $aiResponse"

                val aiMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = messageContent,
                    type = MessageType.ASSISTANT,
                    characterId = aiCharacter.aiCharacterId,
                    chatUserId = USER_ID,
                    conversationId = conversation.id
                )
                // 保存AI消息到数据库 创建一个新的协程来执行挂起函数
                CoroutineScope(Dispatchers.IO).launch {
                    savaMessage(aiMessage, true)
//                    val summaryMessages = getSummaryMessages(conversation)
//                    summarize(conversation, aiCharacter, summaryMessages)
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
        summaryMessages: List<TempChatMessage>,
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
//            val count = tempChatMessageDao.getCountByConversationId(conversation.id)
//            if (count < summarizeTriggerCount) {
////                Log.d(TAG, "无需总结 count=$count")
//                return@withLock
//            }
//            Log.d(TAG, "开始总结 count=$count")

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
                # [对话片段]
            """.trimIndent()
            // 添加历史消息
            for (message in summaryMessages) {
                summaryRequest += "\n${message.content}"
            }
            summaryRequest += "\n现在请你代入${aiCharacter.name}的角色并以“我”自称，代入角色的性格特点，用[回忆的口吻]总结以上对话片段为一段话(直接回复一段话)"
            messages.add(Message("system", summaryRequest))
            // 使用CompletableDeferred来等待API调用完成
            val apiCompleted = CompletableDeferred<Boolean>()
            apiService.sendMessage(
                messages = messages,
                model = configManager.getSelectedModel() ?: "",
                temperature = 0.3f,
                onSuccess = { aiResponse ->
                    // 收到回复
                    val newMemoryContent =
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
                            "aiMemory :$aiMemory"
                        )
                        if (aiMemory == null) {
                            aiMemory = AIChatMemory(
                                id = UUID.randomUUID().toString(),
                                characterId = aiCharacter.aiCharacterId,
                                conversationId = conversation.id,
                                content = newMemoryContent,
                                count = 1,
                                createdAt = System.currentTimeMillis()
                            )
                            val result = aiChatMemoryDao.insert(aiMemory)
                            Log.d(TAG, "总结成功 insert :$result")
                        } else {
                            aiMemory.content =
                                if (aiMemory.content.isNotEmpty() == true) "${aiMemory.content}\n\n$newMemoryContent" else newMemoryContent
                            aiMemory.count += 1
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

    suspend fun getSummaryMessages(conversation: Conversation): List<TempChatMessage> {
        val allHistory = tempChatMessageDao.getByConversationId(conversation.id)
        return allHistory.subList(0, (allHistory.size - maxContextMessageSize).coerceAtLeast(0))
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
            id = "${conversation.id}:${System.currentTimeMillis()}",
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
        // 验证图片的大小是否超过8MB，若超过则循环压缩图片至8MB以下
        val compressedBitmap = BitMapUtil.compressBitmapToLimit(bitmap, 8 * 1024 * 1024)
        // 将压缩后的Bitmap转换为Base64字符串
        val imageBase64 = BitMapUtil.bitmapToBase64(compressedBitmap)
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

    interface AIChatMessageListener {
        fun onMessageSent(message: ChatMessage)
        fun onMessageReceived(message: ChatMessage)
        fun onError(error: String)
        fun onShowToast(message: String)
    }
}