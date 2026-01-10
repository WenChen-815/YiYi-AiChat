package com.wenchen.yiyi.feature.aiChat.common

import android.graphics.Bitmap
import android.net.Uri
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
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
import com.wenchen.yiyi.core.model.network.ChatResponse
import com.wenchen.yiyi.core.model.network.Message
import com.wenchen.yiyi.core.network.service.ChatRequest
import com.wenchen.yiyi.core.network.service.ContentItem
import com.wenchen.yiyi.core.network.service.MultimodalChatRequest
import com.wenchen.yiyi.core.network.service.MultimodalMessage
import com.wenchen.yiyi.core.result.ResultHandler
import com.wenchen.yiyi.core.result.asResult
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.util.BitMapUtil
import com.wenchen.yiyi.core.util.ChatUtil
import com.wenchen.yiyi.core.util.storage.FilesUtil
import com.wenchen.yiyi.feature.worldBook.model.WorldBook
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIChatManager @Inject constructor(
    val userConfigState: UserConfigState,
    val chatMessageRepository: ChatMessageRepository,
    val tempChatMessageRepository: TempChatMessageRepository,
    val aiChatMemoryRepository: AIChatMemoryRepository,
    val aiHubRepository: AiHubRepository
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
            val aiCharacter = aiCharacters.find { it.aiCharacterId == id }
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
            aiCharacter.aiCharacterId,
            conversation.id
        )?.content
        var worldBook: WorldBook? = null
        if (conversation.chatWorldId.isNotEmpty()) {
            // 获取世界ID并通过moshi解析
            val worldBookJson = FilesUtil.readFile("world_book/${conversation.chatWorldId}.json")
            val worldBookAdapter: JsonAdapter<WorldBook> =
                Moshi.Builder().build().adapter(WorldBook::class.java)
            worldBook = worldBookJson.let { worldBookAdapter.fromJson(it) }
        }
        val worldItemBuilder = StringBuilder()
        // 处理世界物品，构建名称到描述的映射和正则模式
        val worldItems = worldBook?.worldItems ?: emptyList()
        val itemNameToDesc = worldItems.associate { it.name to it.desc } // 名称→描述映射
        /*
        知识点：集合操作函数
        associate 和 groupBy 都是 Kotlin 集合操作函数，但它们有不同的用途：
            associate 函数
                作用: 将集合转换为 Map，每个元素映射到一个键值对，遇到重复值则保留最后一个
                特点:每个输入元素产生一个键值对,输出 Map 的大小等于输入集合的大小（除非有重复键）,常见变体包括 associateBy、associateWith 等
            groupBy 函数
                作用: 将集合按照某个键进行分组
                特点:相同键的元素会被归为一组,输出 Map 的值是元素列表,可能产生比原集合更少的键
         */
        // 构建正则模式（转义特殊字符，避免正则语法错误）
        val itemNames = worldItems.map { Regex.escape(it.name) } // 转义特殊字符
        val pattern = if (itemNames.isNotEmpty()) itemNames.joinToString("|")
            .toRegex() else null // 合并为"item1|item2"模式
        val matchedItems = mutableSetOf<String>() // 记录已匹配物品，避免重复添加
        // 添加历史消息
        for (message in oldMessages) {
            if (message.type == MessageType.ASSISTANT && message.characterId == aiCharacter.aiCharacterId) {
                messages.add(Message("assistant", ChatUtil.parseMessage(message).cleanedContent))
            } else {
                messages.add(Message("user", message.content))
            }

            // 使用正则匹配替换嵌套循环
//            if (pattern != null) {
            pattern?.findAll(message.content)?.forEach { matchResult -> // 一次匹配所有物品
                val matchedName = matchResult.value
                if (matchedName !in matchedItems) { // 去重处理
                    itemNameToDesc[matchedName]?.let { desc ->
                        worldItemBuilder.append("[${matchedName}]: $desc\n")
                        matchedItems.add(matchedName)
                    }
                }
            }
        }
        // 添加系统提示消息
        val prompt = buildString {
            if (worldBook != null) {
                append("# [WORLD]\n## 世界介绍\n${worldBook.worldDesc ?: ""}")
            }
            if (worldItemBuilder.isNotEmpty()) {
                append("\n## 物品释义\n${worldItemBuilder}")
            }
            append("\n# [PLAYER]")
            append("\n## 用户角色信息\n-名称：${conversation.playerName} ")
            if (conversation.playGender.isNotBlank()) {
                append("\n-性别:${conversation.playGender} ")
            }
            if (conversation.playerDescription.isNotBlank()) {
                append("\n-描述:${conversation.playerDescription}")
            }
            if (conversation.chatSceneDescription.isNotBlank()) {
                append("\n# [SCENE]当前场景\n${conversation.chatSceneDescription}\n")
            }
            if (conversation.type == ConversationType.SINGLE) {
                append(
                    """
                    # [IMPORTANT]
                    **你应以[${conversation.playerName}]为主要交互对象**
                    **你需要深度理解世界设定、用户角色信息、当前场景以及后续你需要扮演的角色信息**
                    **现在请使用以下[${aiCharacter.name}]的身份参与对话**
                    """.trimIndent()
                )
            }
            if (conversation.type == ConversationType.GROUP) {
                append(
                    """
                    # [IMPORTANT]
                    **你正处于多人对话与行动的环境当中**
                    **你允许与多位角色交互，但仍应以[${conversation.playerName}]为主要交互对象**
                    **你需要深度理解世界设定、用户角色信息、当前场景以及后续你需要扮演的角色信息**
                    **现在请使用以下[${aiCharacter.name}]的身份参与对话**
                    """.trimIndent()
                )
            }
            if (aiCharacter.roleIdentity.isNotBlank()) {
                append("\n# [YOUR ROLE]${aiCharacter.name}\n## 角色任务&身份\n${aiCharacter.roleIdentity}")
            }
            if (aiCharacter.roleAppearance.isNotBlank()) {
                append("\n## 角色外貌\n${aiCharacter.roleAppearance}")
            }
            if (aiCharacter.roleDescription.isNotBlank()) {
                append("\n## 角色描述\n${aiCharacter.roleDescription}")
            }
            append("\n# [MEMORY]以下为角色[${aiCharacter.name}]的记忆:\n$history")
            if (aiCharacter.outputExample.isNotBlank()) {
                append("\n# [EXAMPLE]角色输出示例\n${aiCharacter.outputExample}\n")
            }
            // [PRIORITY 2]在“。 ？ ！ …”等表示句子结束处，或根于语境需要分隔处使用反斜线 (\) 分隔,以确保良好的可读性，但严格要求[]中的内容不允许使用(\)来分隔
            append(
                """
                # [RULES — STRICT]严格遵守以下行为准则
                [PRIORITY 1]记住你要扮演的角色是[${aiCharacter.name}]，请保持这个身份进行对话，不要改变身份
                [PRIORITY 2]收到系统旁白消息时，必须根据其中提示内容进行扩写
                其他规则:
                """.trimIndent()
            )
            if (aiCharacter.behaviorRules.isNotBlank()) {
                append(aiCharacter.behaviorRules)
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
                id = UUID.randomUUID().toString(),
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
                    id = UUID.randomUUID().toString(),
                    content = messageContent,
                    type = MessageType.ASSISTANT,
                    characterId = aiCharacter.aiCharacterId,
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
     * @param messages 要发送的消息列表，包括系统提示、历史消息和用户最新消息
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

        val messageId = UUID.randomUUID().toString()
        val currentDate = SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
        val prefix = if (userConfig?.enableTimePrefix == true) "$currentDate|" else ""
        val currentCharacterName = "[${aiCharacter.name}] "

        // 1. 创建占位消息并通知 UI
        val aiMessage = ChatMessage(
            id = messageId,
            content = "$prefix$currentCharacterName",
            type = MessageType.ASSISTANT,
            characterId = aiCharacter.aiCharacterId,
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
        callback: (() -> Unit)? = null
    ) {
        val userConfig = Application.globalUserConfigState.userConfig.value
        // 添加用户消息到列表
        if (aiCharacter == null) {
            Timber.tag(TAG).e("summarize: 未选择AI角色")
            return
        }

        val characterId = aiCharacter.aiCharacterId
        // 获取该角色对应的锁，如果不存在则创建一个新的锁
        val characterLock = characterLocks.computeIfAbsent(characterId) { Mutex() }
        // 尝试获取角色锁，如果已被锁定则等待
        characterLock.withLock {
            var aiMemory = aiChatMemoryRepository.getByCharacterIdAndConversationId(
                aiCharacter.aiCharacterId,
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
                        **摘要**:$aiResponse
                    """.trimIndent()
                    CoroutineScope(Dispatchers.IO).launch {
                        Timber.tag(TAG).d("总结成功 delete :${summaryMessages.map { it.content }}")
                        // 删除已总结的临时消息
                        tempChatMessageRepository.deleteMessagesByIds(summaryMessages.map { it.id })
                        Timber.tag(TAG).d("aiMemory :$aiMemory")
                        if (aiMemory == null) {
                            aiMemory = AIChatMemory(
                                id = UUID.randomUUID().toString(),
                                characterId = aiCharacter.aiCharacterId,
                                conversationId = conversation.id,
                                content = newMemoryContent,
                                count = 1,
                                createdAt = System.currentTimeMillis()
                            )
                            val result = aiChatMemoryRepository.insert(aiMemory)
                            Timber.tag(TAG).d("总结成功 insert :$result")
                        } else {
                            aiMemory.content =
                                if (aiMemory.content.isNotEmpty()) "${aiMemory.content}\n\n$newMemoryContent" else newMemoryContent
                            aiMemory.count += 1
                            val result = aiChatMemoryRepository.update(aiMemory)
                            Timber.tag(TAG).d("总结成功 update :$result")
                        }
                        callback?.invoke()
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
        val maxContextMessageSize = Application.globalUserConfigState.userConfig.value?.maxContextMessageSize ?: 10
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
            characterId = character.aiCharacterId,
            chatUserId = userConfig.userId ?: "",
            contentType = MessageContentType.IMAGE,
            imgUrl = imgUri.toString(),
            conversationId = conversation.id
        )
        // 通知所有监听器消息已发送
        CoroutineScope(Dispatchers.Main).launch {
            // 通知所有监听器消息已接收
            listeners.forEach { it.onMessageSent(imgMessage) }
        }
        chatMessageRepository.insertMessage(imgMessage)
        // 验证图片的大小是否超过8MB，若超过则循环压缩图片至8MB以下
        val compressedBitmap = BitMapUtil.compressBitmapToLimit(bitmap, 8 * 1024 * 1024)
        // 将压缩后的Bitmap转换为Base64字符串
        val imageBase64 = BitMapUtil.bitmapToBase64(compressedBitmap)
        // 准备提示文本
        val prompt =
            "请用中文描述这张图片的主要内容或主题。不要使用'这是'、'这张'等开头，直接描述。如果有文字，请包含在描述中。"
        // 创建请求体对象
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

    interface AIChatMessageListener {
        fun onMessageSent(message: ChatMessage)
        fun onMessageReceived(message: ChatMessage)
        fun onMessageChunk(messageId: String, chunk: String)
        fun onAllReplyCompleted()
        fun onError(error: String)
        fun onShowToast(message: String)
    }
}