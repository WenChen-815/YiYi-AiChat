package com.wenchen.yiyi.feature.aiChat.common

import android.graphics.Bitmap
import android.net.Uri
import com.wenchen.yiyi.core.database.entity.ChatMessage
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.MessageContentType
import com.wenchen.yiyi.core.database.entity.MessageType
import com.wenchen.yiyi.core.database.entity.TempChatMessage
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.core.database.entity.AIChatMemory
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.core.data.repository.AIChatMemoryRepository
import com.wenchen.yiyi.core.data.repository.AiHubRepository
import com.wenchen.yiyi.core.data.repository.ChatMessageRepository
import com.wenchen.yiyi.core.data.repository.TempChatMessageRepository
import com.wenchen.yiyi.core.data.repository.YiYiWorldBookRepository
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookEntry
import com.wenchen.yiyi.core.model.network.ChatResponse
import com.wenchen.yiyi.core.model.network.Message
import com.wenchen.yiyi.core.network.service.ChatRequest
import com.wenchen.yiyi.core.network.service.ContentItem
import com.wenchen.yiyi.core.network.service.MultimodalChatRequest
import com.wenchen.yiyi.core.network.service.MultimodalMessage
import com.wenchen.yiyi.core.result.ResultHandler
import com.wenchen.yiyi.core.result.asResult
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.util.ui.BitMapUtils
import com.wenchen.yiyi.core.util.business.ChatUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIChatManager @Inject constructor(
    val userConfigState: UserConfigState,
    val chatMessageRepository: ChatMessageRepository,
    val tempChatMessageRepository: TempChatMessageRepository,
    val aiChatMemoryRepository: AIChatMemoryRepository,
    val aiHubRepository: AiHubRepository,
    val yiYiWorldBookRepository: YiYiWorldBookRepository,
    val chatUtils: ChatUtils
) {
    // 基础配置
    private val TAG = "AIChatManager"

    // 并发控制
    private val characterLocks = ConcurrentHashMap<String, Mutex>()
    private val sendCharacterQueue = ConcurrentHashMap<String, MutableList<AICharacter>>()
    private val isProcessing = ConcurrentHashMap<String, Boolean>()
    private val listeners = Collections.synchronizedSet(mutableSetOf<AIChatMessageListener>())

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
        isHandleUserMessage: Boolean,
        oldMessages: List<TempChatMessage>,
        showInChat: Boolean = true,
        isSendSystemMessage: Boolean = false,
        enableStreamOutput: Boolean = userConfigState.userConfig.value?.enableStreamOutput ?: false
    ) : Int {
        // 参数校验
        if (conversation.characterIds.isEmpty() || aiCharacters.isEmpty()) {
            Timber.tag(TAG).e("未选择AI角色")
            return 0
        }
        /*
         * 此处冗余，但予以保留
         * 这里不需要用到userMessage的原因是在handleUserMessage方法中将处理后的消息插入了数据库并通知给了ChatActivity，
         * 而ChatActivity的viewmodel会更新上下文，并且传递的参数本来是就是上下文的引用，所以上下文是最新的，不需要重复添加
        */
        var userMessages: List<Message> = emptyList()
        if (isHandleUserMessage){
            userMessages = handleUserMessage(conversation, newMessageTexts, isSendSystemMessage, showInChat)
        }
        val count = tempChatMessageRepository.getCountByConversationId(conversation.id)
//        Timber.tag(TAG).d("sendGroupMessage: $count")
        val summaryMessages = getSummaryMessages(conversation)
        if (count >= (Application.globalUserConfigState.userConfig.value?.summarizeTriggerCount ?: 20)) aiCharacters.forEach { aiCharacter ->
            summarize(conversation, aiCharacter, summaryMessages)
        }
        // 初始化队列和状态
        val queueKey = conversation.id
        if (!sendCharacterQueue.containsKey(queueKey)) {
            sendCharacterQueue[queueKey] = mutableListOf()
            isProcessing[queueKey] = false
        }

        conversation.characterIds.forEach { (id, chance) ->
            val aiCharacter = aiCharacters.find { it.id == id }
            if (aiCharacter == null) {
                Timber.tag(TAG).e("未找到AI角色: $id")
                return@forEach
            }

            // 检查是否有关键词配置
            val keywordsList = conversation.characterKeywords?.get(id) ?: emptyList()
//            Timber.tag(TAG).d("keywordsList: $keywordsList")
            // 如果没有关键词配置或关键词列表为空，则使用概率判断
            if (conversation.characterKeywords == null || keywordsList.isEmpty()) {
                val random = Math.random()
                if (random < chance) {
                    sendCharacterQueue[queueKey]?.add(aiCharacter)
                }
            } else {
                // 有关键词配置，检查是否匹配
                val keywords = keywordsList.joinToString("|").toRegex()
//                Timber.tag(TAG).d("keywords: $keywords")
                if (keywords.containsMatchIn(newMessageTexts[0])) {
                    // 匹配到关键词，直接加入队列
                    sendCharacterQueue[queueKey]?.add(aiCharacter)
                } else {
                    // 未匹配到关键词，使用概率判断
                    val random = Math.random()
                    if (random < chance) {
                        sendCharacterQueue[queueKey]?.add(aiCharacter)
                    }
                }
            }
        }


        // 启动处理流程
        return processMessageQueue(conversation, oldMessages, enableStreamOutput)
    }

    private suspend fun processMessageQueue(
        conversation: Conversation,
        oldMessages: List<TempChatMessage>,
        enableStreamOutput: Boolean = userConfigState.userConfig.value?.enableStreamOutput ?: false
    ) : Int {
        var replyCount = 0
        val queueKey = conversation.id
        if (isProcessing[queueKey] == true) return 0

        isProcessing[queueKey] = true
        sendCharacterQueue[queueKey]?.shuffle() // 对发送队列进行乱序处理
        // 使用单个协程处理整个队列，使用 async 来获取返回值
        val deferred = CoroutineScope(Dispatchers.IO).async {
            try {
                while (sendCharacterQueue[queueKey]?.isNotEmpty() == true) {
                    val aiCharacter = sendCharacterQueue[queueKey]?.removeAt(0) ?: break
                    val messages = generateBaseMessages(aiCharacter, conversation, oldMessages)

                    val completion = CompletableDeferred<Boolean>()
                    if (enableStreamOutput){
                        sendWithStream(conversation, aiCharacter, messages) { isSuccess ->
                            completion.complete(isSuccess)
                        }
                    } else {
                        sendWithoutStream(conversation, aiCharacter, messages) { isSuccess ->
                            completion.complete(isSuccess)
                        }
                    }
                    // 等待当前消息发送完成
                    if (completion.await()) replyCount++ else break
                    delay((1000).toLong()) // 1s延迟
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "处理消息队列时出错")
            } finally {
                clearMessageQueue(queueKey)
                listeners.forEach { it.onAllReplyCompleted() }
            }
            replyCount
        }
        return deferred.await()
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
     * @param enableStreamOutput 是否启用流式输出，默认为用户配置中的值。
     */
    suspend fun sendMessage(
        conversation: Conversation,
        aiCharacter: AICharacter?,
        newMessageTexts: List<String>,
        isHandleUserMessage: Boolean,
        oldMessages: List<TempChatMessage>,
        showInChat: Boolean = true,
        isSendSystemMessage: Boolean = false,
        enableStreamOutput: Boolean = userConfigState.userConfig.value?.enableStreamOutput ?: false
    ) {
        // 参数校验
        if (aiCharacter == null) {
            Timber.tag(TAG).e("未选择AI角色")
            return
        }
        // 处理新消息
        var userMessages: List<Message> = emptyList()
        if (isHandleUserMessage){
            userMessages = handleUserMessage(conversation, newMessageTexts, isSendSystemMessage, showInChat)
        }
        val messages = generateBaseMessages(aiCharacter, conversation, oldMessages)

        // 发送消息和总结
//        messages.addAll(userMessages)
        if (enableStreamOutput) {
            sendWithStream(conversation, aiCharacter, messages)
        } else {
            sendWithoutStream(conversation, aiCharacter, messages)
        }

        val count = tempChatMessageRepository.getCountByConversationId(conversation.id)
        if (count >= (Application.globalUserConfigState.userConfig.value?.summarizeTriggerCount ?: 20)) {
            val summaryMessages = getSummaryMessages(conversation)
            Timber.tag(TAG).d("开始总结 count=$count")
            summarize(conversation, aiCharacter, summaryMessages)
        }
    }

    private suspend fun generateBaseMessages(
        aiCharacter: AICharacter,
        conversation: Conversation,
        oldMessages: List<TempChatMessage>
    ): MutableList<Message> {
        val messages = mutableListOf<Message>()
        val history = aiChatMemoryRepository.getByCharacterIdAndConversationId(
            aiCharacter.id,
            conversation.id
        )?.content

        val worldBooksWithEntries = if (conversation.chatWorldId.isNotEmpty()) {
            yiYiWorldBookRepository.getBooksWithEntriesByIds(conversation.chatWorldId)
        } else emptyList()

        val worldItemBuilder = StringBuilder()
        val appliedEntryIds = mutableSetOf<String>()
        val keywordEntries = mutableListOf<YiYiWorldBookEntry>()

        // 1. 处理 Constant 条目 (常驻条目)
        worldBooksWithEntries.forEach { bookWithEntries ->
            bookWithEntries.entries.forEach { entry ->
                if (entry.enabled) { // 检查条目是否启用
                    if (entry.constant) { // 判断是否为常驻条目
                        val shouldApply = if (entry.extensions.useProbability) { // 检查是否使用概率设置
                            (Math.random() * 100).toInt() < entry.extensions.probability // 根据概率计算是否应用此条目
                        } else {
                            true // 如果不使用概率，则总是应用
                        }
                        if (shouldApply) { // 如果确定应用此条目
                            if (appliedEntryIds.add(entry.entryId)) { // 确保条目ID未被添加过（避免重复）
                                // 对世界书条目内容进行占位符替换
                                val content = (entry.content ?: "").replacePlaceholders(conversation, aiCharacter)
                                worldItemBuilder.append("$content\n") // 将条目内容添加到世界项构建器中
                            }
                        }
                    } else {
                        keywordEntries.add(entry) // 如果不是常驻条目，则将其添加到关键词条目列表中
                    }
                }
            }
        }

        // 2. 准备关键词匹配
        val keyToEntry = mutableMapOf<String, YiYiWorldBookEntry>() // 创建一个映射，将关键词映射到对应的世界书条目
        val allKeys = mutableListOf<String>() // 创建一个列表存储所有的关键词
        keywordEntries.forEach { entry -> // 遍历所有非常驻关键词条目
            entry.keys.forEach { key -> // 遍历每个条目的所有关键词
                if (key.isNotBlank()) { // 检查关键词是否非空（不是空白）
                    keyToEntry[key] = entry // 将关键词和其对应的世界书条目建立映射关系
                    allKeys.add(Regex.escape(key)) // 将关键词转义后添加到关键词列表中，防止正则表达式特殊字符造成问题
                }
            }
        }

        val pattern = if (allKeys.isNotEmpty()) allKeys.joinToString("|").toRegex() else null

        // 3. 处理消息历史并进行关键词匹配
        for (message in oldMessages) {
            // 对历史消息内容进行占位符替换
            val processedContent = message.content.replacePlaceholders(conversation, aiCharacter)
            
            if (message.type == MessageType.ASSISTANT && message.characterId == aiCharacter.id) {
                // 对于 AI 回复，需要解析并处理清洗后的内容
                val parsedContent = chatUtils.parseMessage(message).cleanedContent.replacePlaceholders(conversation, aiCharacter)
                messages.add(Message("assistant", parsedContent))
            } else {
                messages.add(Message("user", processedContent))
            }

            // 匹配关键词（基于原始内容或处理后的内容，通常关键词不含占位符）
            pattern?.findAll(message.content)?.forEach { matchResult -> // 在消息内容中查找所有匹配关键词模式的内容
                val matchedKey = matchResult.value // 获取匹配到的具体关键词
                val entry = keyToEntry[matchedKey] // 根据匹配到的关键词查找对应的世界书条目
                if (entry != null && appliedEntryIds.add(entry.entryId)) { // 如果找到了对应的条目且该条目尚未被添加过
                    val entryContent = (entry.content ?: "").replacePlaceholders(conversation, aiCharacter)
                    worldItemBuilder.append("$entryContent\n") // 将匹配到的条目内容追加到世界项构建器中
                }
            }
        }

        // 添加系统提示消息
        val prompt = buildString {
//            if (worldBooksWithEntries.isNotEmpty()) {
//                append("# [WORLD]\n")
//                worldBooksWithEntries.forEach { bookWithEntries ->
//                    append("## 世界介绍: ${bookWithEntries.book.name ?: ""}\n${bookWithEntries.book.description ?: ""}\n")
//                }
//            }
            if (worldItemBuilder.isNotEmpty()) {
                append(worldItemBuilder.toString())
            }
            append("\n# [PLAYER]")
            append("\n## 用户角色信息\n-昵称：${conversation.playerName} ")
            if (conversation.playGender.isNotBlank()) {
                append("\n-性别:${conversation.playGender} ")
            }
            if (conversation.playerDescription.isNotBlank()) {
                append("\n-描述:${conversation.playerDescription}")
            }
            if (conversation.chatSceneDescription.isNotBlank()) {
                append("\n# [SCENE]当前场景\n${conversation.chatSceneDescription}\n")
            }
//            if (conversation.type == ConversationType.SINGLE) {
//                append(
//                    """
//                    # [IMPORTANT]
//                    **你应以[${conversation.playerName}]为主要交互对象**
//                    **你需要深度理解世界设定、用户角色信息、当前场景以及后续你需要扮演的角色信息**
//                    **现在请使用以下[${aiCharacter.name}]的身份参与对话**
//                    """.trimIndent()
//                )
//            }
            if (conversation.type == ConversationType.GROUP) {
                append(
                    // **你需要深度理解世界设定、用户角色信息、当前场景以及后续你需要扮演的角色信息**
                    // **现在请使用以下[${aiCharacter.name}]的身份参与对话**
                    """
                    # [IMPORTANT]
                    **你正处于多人对话与行动的环境当中**
                    **你允许与多位角色交互，但仍应以[${conversation.playerName}]为主要交互对象**
                    """.trimIndent()
                )
            }
            
            // 融合后的描述字段
            if (aiCharacter.description.isNotBlank()) {
                append("\n# [YOUR ROLE]${aiCharacter.name}\n## 角色设定\n${aiCharacter.description.replacePlaceholders(conversation, aiCharacter)}")
            }
            
            // 其他可选字段
            aiCharacter.personality?.takeIf { it.isNotBlank() }?.let {
                append("\n## 角色性格\n${it.replacePlaceholders(conversation, aiCharacter)}")
            }
            aiCharacter.scenario?.takeIf { it.isNotBlank() }?.let {
                append("\n## 角色场景\n${it.replacePlaceholders(conversation, aiCharacter)}")
            }

            append("\n# [MEMORY]以下为角色[${aiCharacter.name}]的记忆:\n ===MEMORY START===\n$history\n ===MEMORY END===\n")
            
            if (aiCharacter.mes_example?.isNotBlank() == true) {
                append("\n# [EXAMPLE]输出示例\n${aiCharacter.mes_example.replacePlaceholders(conversation, aiCharacter)}\n")
            }
//            append(
//                """
//                # [RULES — STRICT]严格遵守以下行为准则
//                [PRIORITY 1]记住你要扮演的是[${aiCharacter.name}]，请保持这个身份进行对话，不要改变身份
//                [PRIORITY 2]收到系统旁白消息时，必须根据其中提示内容进行扩写
//                其他规则:
//                """.trimIndent()
//            )
            
            // 系统提示和历史指令
            aiCharacter.system_prompt?.takeIf { it.isNotBlank() }?.let {
                append("\n${it.replacePlaceholders(conversation, aiCharacter)}")
            }
            aiCharacter.post_history_instructions?.takeIf { it.isNotBlank() }?.let {
                append("\n${it.replacePlaceholders(conversation, aiCharacter)}")
            }
        }.trim()
        
        if (prompt.isNotEmpty()) {
            messages.add(0, Message("system", "$prompt\n"))
        }
        return messages
    }

    private suspend fun savaMessage(message: ChatMessage, saveToTemp: Boolean) {
        chatMessageRepository.insertMessage(message)
        if (saveToTemp) {
            tempChatMessageRepository.insert(
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
        val userConfig = Application.globalUserConfigState.userConfig.value
        val userMessages = mutableListOf<Message>()
        val currentDate =
            SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
        // 根据配置决定是否添加时间戳
        val prefix = if (userConfig?.enableTimePrefix == true) "$currentDate|" else ""
        val currentUserName = "[${conversation.playerName}]"
        for (newMessageText in newMessageTexts) {
            val newContent = if (isSendSystemMessage) {
                "$prefix[系统旁白] $newMessageText"
            } else {
                "$prefix${currentUserName} $newMessageText"
            }
            val userMessage = ChatMessage(
                id = NanoIdUtils.randomNanoId(),
                content = newContent,
                type = if (isSendSystemMessage) MessageType.SYSTEM else MessageType.USER,
                characterId = "user",
                chatUserId = userConfig?.userId ?: "",
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
     * 非流式
     * 发送消息给AI模型并处理响应的核心方法
     *
     * @param conversation 当前聊天会话对象，包含会话相关信息
     * @param aiCharacter AI角色对象，包含角色身份、描述等配置信息
     * @param messages 要发送的消息列表，包括系统提示、历史消息和用户最新消息
     * @param afterSend 发送完成后的回调函数，用于通知调用方是否成功
     */
    private fun sendWithoutStream(
        conversation: Conversation,
        aiCharacter: AICharacter?,
        messages: MutableList<Message>,
        afterSend:(Boolean) -> Unit = {}
    ) {
        val userConfig = Application.globalUserConfigState.userConfig.value
        if (aiCharacter == null) {
            Timber.tag(TAG).e("未选择AI角色")
            return
        }
//        Timber.tag(TAG).d("send to ${aiCharacter.name}\n%s", messages.joinToString("\n"))
        val chatRequest = ChatRequest(userConfig?.selectedModel ?: "", messages, 1.0f)
        ResultHandler.handleResultWithData<ChatResponse>(
            scope = CoroutineScope(Dispatchers.IO),
            flow = aiHubRepository.sendMessage(
                baseUrl = userConfig?.baseUrl,
                apiKey = userConfig?.baseApiKey,
                request = chatRequest
            ).asResult(),
            onLoading = {},
            onData = { chatResponse ->
                val currentDate =
                    SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
                val prefix = if (userConfig?.enableTimePrefix == true) "$currentDate|" else ""
                val currentCharacterName = "[${aiCharacter.name}]"
                val responseText = chatResponse.choices?.firstOrNull()?.message?.content
                val messageContent = "$prefix${currentCharacterName} $responseText"
                val aiMessage = ChatMessage(
                    id = NanoIdUtils.randomNanoId(),
                    content = messageContent,
                    type = MessageType.ASSISTANT,
                    characterId = aiCharacter.id,
                    chatUserId = userConfig?.userId ?: "",
                    conversationId = conversation.id
                )
                // 保存AI消息到数据库 创建一个新的协程来执行挂起函数
                CoroutineScope(Dispatchers.IO).launch {
                    savaMessage(aiMessage, true)
//                    val summaryMessages = getSummaryMessages(conversation)
//                    summarize(conversation, aiCharacter, summaryMessages)
                    afterSend.invoke(true)
                    listeners.forEach { it.onMessageReceived(aiMessage) }
                    listeners.forEach { it.onAllReplyCompleted() }
                }
//                Timber.tag(TAG).d("AI回复:${aiMessage.content}")
            },
            onError = { messages, exception ->
                CoroutineScope(Dispatchers.Main).launch {
                    listeners.forEach { it.onError("$messages \n $exception") }
                }
                afterSend.invoke(false)
            },
            onFinally = {}
        )
    }

    /**
     * 流式
     * 发送消息给AI模型并处理响应的核心方法
     *
     * @param conversation 当前聊天会话对象，包含会话相关信息
     * @param aiCharacter AI角色对象，包含角色身份、描述等配置信息
     * @param messages 要发送的消息列表，包括系统提示、历史消息 and 用户最新消息
     * @param afterSend 发送完成后的回调函数，用于通知调用方是否成功
     */
    private suspend fun sendWithStream(
        conversation: Conversation,
        aiCharacter: AICharacter?,
        messages: MutableList<Message>,
        afterSend: (Boolean) -> Unit = {}
    ) {
        val userConfig = Application.globalUserConfigState.userConfig.value
        if (aiCharacter == null) {
            Timber.tag(TAG).e("未选择AI角色")
            return
        }

        val messageId = NanoIdUtils.randomNanoId()
        val currentDate = SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
        val prefix = if (userConfig?.enableTimePrefix == true) "$currentDate|" else ""
        val currentCharacterName = "[${aiCharacter.name}] "

        // 1. 创建占位消息并通知 UI
        val aiMessage = ChatMessage(
            id = messageId,
            content = "$prefix$currentCharacterName",
            type = MessageType.ASSISTANT,
            characterId = aiCharacter.id,
            chatUserId = userConfig?.userId ?: "",
            conversationId = conversation.id
        )

        CoroutineScope(Dispatchers.Main).launch {
            listeners.forEach { it.onMessageReceived(aiMessage) }
        }

        // 2. 发起流式请求
        val chatRequest = ChatRequest(userConfig?.selectedModel ?: "", messages, 1.0f)
        var fullContent = ""
        
        try {
            aiHubRepository.streamSendMessage(
                baseUrl = userConfig?.baseUrl,
                apiKey = userConfig?.baseApiKey,
                request = chatRequest
            ).collect { chunk ->
                fullContent += chunk
                CoroutineScope(Dispatchers.Main).launch {
                    listeners.forEach { it.onMessageChunk(messageId, chunk) }
                }
            }

            // 3. 流式结束，保存完整内容到数据库
            val finalMessage = aiMessage.copy(content = "$prefix$currentCharacterName$fullContent")
            savaMessage(finalMessage, true)
            afterSend.invoke(true)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "流式发送失败")
            CoroutineScope(Dispatchers.Main).launch {
                listeners.forEach { it.onError("${e.message}") }
            }
            afterSend.invoke(false)
        } finally {
            CoroutineScope(Dispatchers.Main).launch {
                listeners.forEach { it.onAllReplyCompleted() }
            }
        }
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
        callback: () -> Unit = {}
    ) {
        val userConfig = Application.globalUserConfigState.userConfig.value
        if (aiCharacter == null) {
            Timber.tag(TAG).e("summarize: 未选择AI角色")
            return
        }

        val characterId = aiCharacter.id
        // 获取该角色对应的锁，如果不存在则创建一个新的锁
        val characterLock = characterLocks.computeIfAbsent(characterId) { Mutex() }
        // 尝试获取角色锁，如果已被锁定则等待
        characterLock.withLock {
            var aiMemory = aiChatMemoryRepository.getByCharacterIdAndConversationId(
                aiCharacter.id,
                conversation.id
            )
            aiMemory?.count?.let {
                if (it >= (userConfig?.maxSummarizeCount ?: 20)) {
                    Timber.tag(TAG).d("已达到最大总结次数")
                    CoroutineScope(Dispatchers.Main).launch {
                        listeners.forEach { listener ->
                            listener.onShowToast("已达到最大总结次数")
                        }
                    }
                    return@withLock
                }
            }

            // 构建消息列表
            val messages = mutableListOf<Message>()
            val currentDate =
                SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
            val summaryPrompt: String
            val historyMessage = summaryMessages.joinToString("\n") { it.content }
//            Timber.tag(TAG).e("即将总结: $historyMessage")
            // 早期additionalSummaryRequirement是作为额外总结要求加入预设的提示词中的
            // 现在则是判断这个字段是否有内容，如果有则替换原来预设的提示词，以达到用户自定义总结格式
            // 由于字段对用户不可见，懒得改名了😄
            if (conversation.additionalSummaryRequirement?.isNotBlank() ?: false) {
                summaryPrompt = conversation.additionalSummaryRequirement?.trim() ?: ""
            } else {
                summaryPrompt = """
                # [Role 角色设定]
                    你是一个绝对客观、冷得像机器一样的“剧情记录员”。你的任务是阅读对话历史，提取出两人之间发生的实质性互动，并将其压缩为精简的“记忆条目”。
                    
                # [Task 任务要求] 请分析对话内容，生成一份记忆清单。请严格遵守以下规则：
                    - 极度客观：只陈述发生的动作、说过的关键台词和产生的后果。不要进行文学修饰，不要发表道德评判，不要写模糊的心理描写（除非是明确表达出来的情绪）。
                    - 保留关键台词：如果是具有侮辱性、命令性、定义性或标志性的对话，必须用引号原文摘录（例如：“笨蛋”、“真乖”等）。这是为了保留互动的“颗粒度”。
                    - 动作具体化：不要只说“他们互动了”，要明确说是“扇巴掌”、“牵手”、“言语辱骂”等具体行为。
                    - 结构统一：每一条记忆都需要记录 [主动方] 对 [被动方] 进行了 [具体行为/评价] ([附带的台词或细节])。
                    - 包含状态定义：如果对话中出现了对两人关系的重新定义（如确立主奴关系、给予特定称呼），必须单独列出。
                    
                # [Input Data 待分析对话]
                    <Conversation_History>
                        {{historyMessage}}
                    </Conversation_History>
                    
                # [Output Format 输出格式]
                    - 请以 Markdown 无序列表输出，每一行代表一个独立的交互事件：

                # [Output Example 输出示例]
                    <example-1>
                        - A辱骂B是垃圾、废物。
                        - A强迫B完成了羞辱性的动作舔鞋底，B很意外A的内心如此黑暗。
                        - A对B的服从表现出满意，并给予了口头奖励（“做得好”）。
                    </example-1>
                    <example-2>
                        - A牵起了B的手，说“我们会长长久久”。
                        - B为A做了一大碗清汤面，味道很淡，但他们吃的很开心。
                        - A趁机拿出了求婚戒指，B难掩内心激动，伸手让A戴上戒指。
                    </example-2>
                    <example-3>
                        - A、B、C三人来到了新的据点，这里很陈旧，但所幸仍然坚固。
                        - C给A和B分配了任务，A需要马上加固门窗，B需要尽快生起火堆，三个人各自分工目标明确，准备度过今夜。
                        - 三人围坐在火堆前，拿出了末日下的“稀罕货”——啤酒，
                    </example-3>
                """.trimIndent()
            }
            // 在发送给 API 之前应用占位符替换
            val finalSummaryRequest = summaryPrompt.replace("{{historyMessage}}", historyMessage).replacePlaceholders(conversation, aiCharacter)
            messages.add(Message("system", finalSummaryRequest))
            
            // 使用CompletableDeferred来等待API调用完成
            val apiCompleted = CompletableDeferred<Boolean>()
            val chatRequest = ChatRequest(userConfig?.selectedModel ?: "", messages, 0.3f)
            ResultHandler.handleResultWithData<ChatResponse>(
                scope = CoroutineScope(Dispatchers.IO),
                flow = aiHubRepository.sendMessage(
                    baseUrl = userConfig?.baseUrl,
                    apiKey = userConfig?.baseApiKey,
                    request = chatRequest).asResult(),
                onData = { chatResponse ->
                    val aiResponse = chatResponse.choices?.firstOrNull()?.message?.content ?: ""
                    val newMemoryContent =
                        """
                        ## 记忆片段 [${currentDate}]
                        $aiResponse
                    """.trimIndent()
                    CoroutineScope(Dispatchers.IO).launch {
//                        Timber.tag(TAG).d("总结成功 delete :${summaryMessages.map { it.content }}")
                        // 删除已总结的临时消息
                        tempChatMessageRepository.deleteMessagesByIds(summaryMessages.map { it.id })
//                        Timber.tag(TAG).d("aiMemory :$aiMemory")
                        if (aiMemory == null) {
                            aiMemory = AIChatMemory(
                                id = NanoIdUtils.randomNanoId(),
                                characterId = aiCharacter.id,
                                conversationId = conversation.id,
                                content = newMemoryContent,
                                count = 1,
                                createdAt = System.currentTimeMillis()
                            )
                            val result = aiChatMemoryRepository.insert(aiMemory)
                            Timber.tag(TAG).d("总结成功[Insert]: $result")
                        } else {
                            aiMemory.content =
                                if (aiMemory.content.isNotEmpty()) "${aiMemory.content}\n\n$newMemoryContent" else newMemoryContent
                            aiMemory.count += 1
                            val result = aiChatMemoryRepository.update(aiMemory)
                            Timber.tag(TAG).d("总结成功[Update]: $result")
                        }
                        callback.invoke()
                        apiCompleted.complete(true)
                    }
                },
                onError = { messages, exception ->
                    Timber.tag(TAG).e(exception, "总结失败：$messages")
                    apiCompleted.complete(false)
                }
            )
            // 等待API调用完成后再释放锁
            apiCompleted.await()
        }
    }

    suspend fun getSummaryMessages(conversation: Conversation): List<TempChatMessage> {
        val allHistory = tempChatMessageRepository.getByConversationId(conversation.id)
        val maxContext = Application.globalUserConfigState.userConfig.value?.maxContextMessageSize ?: 10

        // 计算可以被总结的消息结束索引
        val endIndex = (allHistory.size - maxContext).coerceAtLeast(0)

        // 添加详细日志，帮助排查配置问题
        Timber.tag(TAG).d("获取总结消息: 历史总数=${allHistory.size}, 保留上下文=$maxContext, 待总结数=$endIndex")

        if (endIndex == 0) {
            Timber.tag(TAG).w("警告: 待总结的消息数量为0，请检查设置中的'触发总结数'是否大于'上下文保留数'")
            return emptyList()
        }

        return allHistory.subList(0, endIndex)
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
        val userConfig = Application.globalUserConfigState.userConfig.value
        // 检查图片识别是否启用
        if (userConfig?.imgRecognitionEnabled != true) {
            Timber.tag(TAG).e("图片识别功能未启用")
            CoroutineScope(Dispatchers.Main).launch {
                listeners.forEach { it.onError("图片识别功能未启用") }
            }
            return
        }

        // 检查配置是否完整
        if (userConfig.imgApiKey.isNullOrEmpty() || userConfig.imgBaseUrl.isNullOrEmpty() || userConfig.selectedImgModel.isNullOrEmpty()) {
            Timber.tag(TAG).e("图片识别配置不完整")
            CoroutineScope(Dispatchers.Main).launch {
                listeners.forEach { it.onError("图片识别配置不完整") }
            }
            return
        }

        val imgMessage = ChatMessage(
            id = "${conversation.id}:${System.currentTimeMillis()}",
            content = "发送了图片:[$imgUri]",
            type = MessageType.USER,
            characterId = character.id,
            chatUserId = userConfig.userId,
            contentType = MessageContentType.IMAGE,
            imgUrl = imgUri.toString(),
            conversationId = conversation.id
        )
        CoroutineScope(Dispatchers.Main).launch {
            listeners.forEach { it.onMessageSent(imgMessage) }
        }
        chatMessageRepository.insertMessage(imgMessage)
        // 验证图片的大小是否超过8MB，若超过则循环压缩图片至8MB以下
        val compressedBitmap = BitMapUtils.compressBitmapToLimit(bitmap, 8 * 1024 * 1024)
        // 将压缩后的Bitmap转换为Base64字符串
        val imageBase64 = BitMapUtils.bitmapToBase64(compressedBitmap)
        val prompt =
            "请用中文描述这张图片的主要内容或主题。不要使用'这是'、'这张'等开头，直接描述。如果有文字，请包含在描述中。"
        val contentItems = mutableListOf<ContentItem>().apply {
            add(ContentItem("text", prompt))
            add(ContentItem(type = "image_url", image_url = mapOf("url" to "data:image/jpeg;base64,$imageBase64")))
        }

        val message = MultimodalMessage("user", contentItems)
        val messages = listOf(message)

        val chatRequest = MultimodalChatRequest(userConfig.selectedImgModel ?: "", messages, 0.8f)
        // 调用API发送图片识别请求
        ResultHandler.handleResultWithData<ChatResponse>(
            scope = CoroutineScope(Dispatchers.IO),
            flow = aiHubRepository.sendMultimodalMessage(
                baseUrl = userConfig.imgBaseUrl,
                apiKey = userConfig.imgApiKey,
                request = chatRequest
            ).asResult(),
            onData = { response ->
                val imgDescription = response.choices?.firstOrNull()?.message?.content
                CoroutineScope(Dispatchers.IO).launch {
                    val desc = "[${conversation.playerName}]向你发送了图片:[$imgDescription]"
                    sendMessage(conversation, character, listOf(desc), true, oldMessages, false)
                }
            },
            onError = { messages, exception ->
                Timber.tag(TAG).e(exception, "图片识别失败：$messages")
                CoroutineScope(Dispatchers.Main).launch {
                    listeners.forEach { it.onError("图片识别失败：$messages \n $exception") }
                }
            }
        )
    }

    /**
     * 替换文本中的占位符 {{user}} 和 {{char}}
     */
    private fun String.replacePlaceholders(conversation: Conversation, aiCharacter: AICharacter): String {
        return this.replace("{{user}}", conversation.playerName)
            .replace("{{char}}", aiCharacter.name)
    }

    interface AIChatMessageListener {
        fun onMessageSent(message: ChatMessage)
        fun onMessageReceived(message: ChatMessage)
        fun onMessageChunk(messageId: String, chunk: String)
        fun onAllReplyCompleted()
        fun onError(error: String)
        fun onShowToast(message: String)
    }
}