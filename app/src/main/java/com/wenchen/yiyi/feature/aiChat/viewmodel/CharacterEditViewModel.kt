package com.wenchen.yiyi.feature.aiChat.viewmodel

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.data.repository.AICharacterRepository
import com.wenchen.yiyi.core.data.repository.AIChatMemoryRepository
import com.wenchen.yiyi.core.data.repository.ConversationRepository
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.util.ui.ToastUtils
import com.wenchen.yiyi.core.datastore.storage.ImageManager
import com.wenchen.yiyi.core.database.entity.AIChatMemory
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.feature.output.model.CharaCardV3
import com.wenchen.yiyi.core.util.business.CharaCardParser
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.routes.AiChatRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.wenchen.yiyi.core.data.repository.YiYiRegexGroupRepository
import com.wenchen.yiyi.core.data.repository.YiYiRegexScriptRepository
import com.wenchen.yiyi.core.data.repository.YiYiWorldBookEntryRepository
import com.wenchen.yiyi.core.data.repository.YiYiWorldBookRepository
import com.wenchen.yiyi.core.database.entity.WorldBookEntryExtensions
import com.wenchen.yiyi.core.database.entity.YiYiRegexGroup
import com.wenchen.yiyi.core.database.entity.YiYiRegexScript
import com.wenchen.yiyi.core.database.entity.YiYiWorldBook
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookEntry
import com.wenchen.yiyi.core.util.common.AppJson
import com.wenchen.yiyi.core.util.ui.BitMapUtils
import com.wenchen.yiyi.feature.output.model.CharaExtensionModel
import com.wenchen.yiyi.feature.output.model.CharacterExtensions
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CharacterEditViewModel @Inject constructor(
    private val aiCharacterRepository: AICharacterRepository,
    private val aiChatMemoryRepository: AIChatMemoryRepository,
    private val conversationRepository: ConversationRepository,
    private val yiyiRegexGroupRepository: YiYiRegexGroupRepository,
    private val yiyiRegexScriptRepository: YiYiRegexScriptRepository,
    private val yiyiWorldBookRepository: YiYiWorldBookRepository,
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    savedStateHandle: SavedStateHandle
) : BaseViewModel(
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState,
) {
    // ---------------- 基础 ID 状态 ----------------
    val _characterId = MutableStateFlow("")
    val characterId: StateFlow<String> = _characterId.asStateFlow()

    val _conversationId = MutableStateFlow("")
    var conversationId: StateFlow<String> = _conversationId.asStateFlow()

    val _isNewCharacter = MutableStateFlow(false)
    val isNewCharacter: StateFlow<Boolean> = _isNewCharacter.asStateFlow()

    // ---------------- 编辑中的核心状态 ----------------
    // 1. 角色实体状态
    private val _currentCharacter = MutableStateFlow<AICharacter?>(null)
    val currentCharacter: StateFlow<AICharacter?> = _currentCharacter.asStateFlow()

    // 2. 角色记忆状态
    private val _memory = MutableStateFlow("")
    val memory: StateFlow<String> = _memory.asStateFlow()

    private val _memoryCount = MutableStateFlow(0)
    val memoryCount: StateFlow<Int> = _memoryCount.asStateFlow()

    // 3. 对话/玩家配置状态
    private val _chatWorldId = MutableStateFlow<List<String>>(emptyList())
    val chatWorldId: StateFlow<List<String>> = _chatWorldId.asStateFlow()

    private val _parsedRegexGroup = MutableStateFlow<YiYiRegexGroup?>(null)
    val parsedRegexGroup: StateFlow<YiYiRegexGroup?> = _parsedRegexGroup.asStateFlow()

    private val _saveRegexGroupFlag = MutableStateFlow<Boolean?>(null)
    val saveRegexGroupFlag: StateFlow<Boolean?> = _saveRegexGroupFlag.asStateFlow()

    private val _parsedRegexScripts = MutableStateFlow<List<YiYiRegexScript>>(emptyList())
    val parsedRegexScripts: StateFlow<List<YiYiRegexScript>> = _parsedRegexScripts.asStateFlow()

    private val _parsedWorldBook = MutableStateFlow<YiYiWorldBook?>(null)
    val parsedWorldBook: StateFlow<YiYiWorldBook?> = _parsedWorldBook.asStateFlow()

    private val _parsedWorldBookEntries = MutableStateFlow<List<YiYiWorldBookEntry>>(emptyList())
    val parsedWorldBookEntries: StateFlow<List<YiYiWorldBookEntry>> =
        _parsedWorldBookEntries.asStateFlow()

    private val _saveWorldBookFlag = MutableStateFlow<Boolean?>(null)
    val saveWorldBookFlag: StateFlow<Boolean?> = _saveWorldBookFlag.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    val allWorldBooks: StateFlow<List<YiYiWorldBook>> = yiyiWorldBookRepository.getAllBooksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val imageManager = ImageManager()

    // 用于存储选择的图片
    var avatarBitmap by mutableStateOf<Bitmap?>(null)
    var backgroundBitmap by mutableStateOf<Bitmap?>(null)
    var hasNewAvatar by mutableStateOf(false)
    var hasNewBackground by mutableStateOf(false)

    init {
        val route = savedStateHandle.toRoute<AiChatRoutes.CharacterEdit>()
        _isNewCharacter.value = route.isNewCharacter
        if (isNewCharacter.value) {
            _characterId.value = NanoIdUtils.randomNanoId()
            _currentCharacter.value = AICharacter(
                id = _characterId.value,
                name = "未命名角色",
                userId = userConfigState.userConfig.value?.userId ?: "123123",
                description = "",
                first_mes = null,
                mes_example = null,
                personality = null,
                scenario = null,
                creator_notes = null,
                system_prompt = null,
                post_history_instructions = null,
                avatar = null,
                background = null,
                creation_date = System.currentTimeMillis(),
                modification_date = System.currentTimeMillis(),
            )
        } else {
            _characterId.value = route.characterId
        }
        _conversationId.value = "${userConfigState.userConfig.value?.userId}_${characterId.value}"

        // 初始化时加载数据
        loadCharacterData()
    }

    /**
     * 通用角色实体更新方法
     */
    // 更新角色的方法
    fun updateCharacter(updater: (AICharacter) -> AICharacter) {
        _currentCharacter.value = _currentCharacter.value?.let(updater)
    }

    fun updateMemory(content: String) { _memory.value = content }
    fun updateMemoryCount(count: Int) { _memoryCount.value = count }

    /**
     * 加载数据并填充到 ViewModel 状态中
     */
    fun loadCharacterData() {
        if (isNewCharacter.value) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val charId = _characterId.value
                val convId = _conversationId.value

                val character = aiCharacterRepository.getCharacterById(charId)
                val memoryObj = aiChatMemoryRepository.getByCharacterIdAndConversationId(charId, convId)
                val conversation = conversationRepository.getById(convId)

                withContext(Dispatchers.Main) {
                    _currentCharacter.value = character
                    _memory.value = memoryObj?.content ?: ""
                    _memoryCount.value = memoryObj?.count ?: 0

                    conversation?.let {
                        _chatWorldId.value = it.chatWorldId
                    }
                }
            } catch (e: Exception) {
                Timber.tag("CharacterEditViewModel").e(e, "加载角色数据失败")
            }
        }
    }

    /**
     * 处理图片选择结果
     */
    fun handleImageResult(
        activity: Activity,
        result: ActivityResult,
        onImageSelected: (Bitmap) -> Unit,
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
                    onImageSelected(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onError("图片加载失败: ${e.message}")
                }
            } else {
                onError("未获取到图片URI")
            }
        }
    }

    /**
     * 从图片导入角色数据
     */
    fun importCharacterFromImage(
        context: Context,
        uri: Uri,
    ) {
        _selectedImageUri.value = uri
        viewModelScope.launch(Dispatchers.IO) {
            val (cardV3, extension) = CharaCardParser.readTavernCardAndLargeDataFromUri<CharaCardV3, CharaExtensionModel>(
                context,
                uri
            )
            withContext(Dispatchers.Main) {
                if (cardV3 != null) {
                    try {
                        val characterData = cardV3.data
                        // 存储解析角色
                        _currentCharacter.value = AICharacter(
                            id = NanoIdUtils.randomNanoId(),
                            name = characterData.name,
                            userId = userState.userId.value,
                            description = characterData.description,
                            first_mes = characterData.first_mes,
                            mes_example = characterData.mes_example,
                            personality = characterData.personality,
                            scenario = characterData.scenario,
                            creator_notes = characterData.creator_notes,
                            system_prompt = characterData.system_prompt,
                            post_history_instructions = characterData.post_history_instructions,
                            avatar = null,
                            background = null,
                            creation_date = System.currentTimeMillis(),
                            modification_date = System.currentTimeMillis(),
                        )

                        // 从图片加载位图作为头像
                        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(
                                    context.contentResolver,
                                    uri
                                )
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        }

                        // 优先从扩展数据中恢复位图，如果没有则使用原图补偿
                        avatarBitmap =
                            extension?.avatarByte?.let { BitMapUtils.byteArrayToBitmap(it) }
                                ?: originalBitmap
                        backgroundBitmap =
                            extension?.backgroundByte?.let { BitMapUtils.byteArrayToBitmap(it) }
                                ?: originalBitmap
                        hasNewBackground = true
                        hasNewAvatar = true

                        // 恢复记忆
                        _memory.value = extension?.memory ?: ""

                        // 尝试查找正则组
                        if (characterData.extensions != null) {
                            try {
                                val regexExtension = AppJson.decodeFromString<CharacterExtensions>(
                                    characterData.extensions.toString()
                                )
                                Timber.tag("CharacterEditViewModel").i("发现正则扩展！")
                                val regexGroupId = NanoIdUtils.randomNanoId()
                                _parsedRegexGroup.value = YiYiRegexGroup(
                                    id = regexGroupId,
                                    name = "${currentCharacter.value?.name} 的正则",
                                    description = "${currentCharacter.value?.name} 的正则",
                                    source_owner = regexExtension.source_owner,
                                    createTime = System.currentTimeMillis(),
                                    updateTime = System.currentTimeMillis(),
                                )
                                _parsedRegexScripts.value =
                                    regexExtension.regex_scripts?.map { script ->
                                        YiYiRegexScript(
                                            id = NanoIdUtils.randomNanoId(),
                                            groupId = regexGroupId,
                                            scriptName = script.scriptName,
                                            findRegex = script.findRegex,
                                            replaceString = script.replaceString,
                                            trimStrings = script.trimStrings,
                                            placement = script.placement,
                                            disabled = script.disabled,
                                            markdownOnly = script.markdownOnly,
                                            promptOnly = script.promptOnly,
                                            runOnEdit = script.runOnEdit,
                                            substituteRegex = script.substituteRegex,
                                            minDepth = script.minDepth,
                                            maxDepth = script.maxDepth
                                        )
                                    } ?: emptyList()
                                _saveRegexGroupFlag.value = true
                            } catch (e: Exception) {
                                Timber.tag("CharacterEditViewModel").e(e, "解析正则扩展失败")
                            }
                        }

                        // 尝试查找世界书
                        if (characterData.character_book != null) {
                            try {
                                val characterBook = characterData.character_book
                                Timber.tag("CharacterEditViewModel").i("发现世界书！")
                                val worldBookId = NanoIdUtils.randomNanoId()
                                val bookName =
                                    characterBook.name?.ifEmpty { "${characterData.name} 的世界书" }
                                        ?: "${characterData.name} 的世界书"
                                val bookDescription = characterBook.description
                                    ?: "${characterData.name} 的世界书"
                                _parsedWorldBook.value = YiYiWorldBook(
                                    id = worldBookId,
                                    name = bookName,
                                    description = bookDescription,
                                    createTime = System.currentTimeMillis(),
                                    updateTime = System.currentTimeMillis(),
                                )
                                _parsedWorldBookEntries.value =
                                    characterBook.entries.map { entry ->
                                        val extensions = entry.extensions
                                        YiYiWorldBookEntry(
                                            entryId = NanoIdUtils.randomNanoId(),
                                            bookId = worldBookId,
                                            keys = entry.keys,
                                            content = entry.content,
                                            enabled = entry.enabled,
                                            useRegex = entry.use_regex ?: false,
                                            insertionOrder = entry.insertion_order,
                                            name = entry.name,
                                            comment = entry.comment,
                                            selective = entry.selective ?: false,
                                            caseSensitive = entry.case_sensitive ?: false,
                                            constant = entry.constant ?: true,
                                            position = entry.position ?: "before_char",
                                            displayIndex = entry.display_index ?: 1,
                                            extensions = WorldBookEntryExtensions(
                                                selectiveLogic = extensions?.selectiveLogic ?: 0,
                                                position = extensions?.position ?: 0,
                                                depth = extensions?.depth ?: 4,
                                                role = extensions?.role ?: 0,
                                                matchWholeWords = extensions?.match_whole_words
                                                    ?: true,
                                                probability = extensions?.probability ?: 100,
                                                useProbability = extensions?.useProbability ?: true,
                                                sticky = extensions?.sticky ?: 0,
                                                cooldown = extensions?.cooldown ?: 0,
                                                delay = extensions?.delay ?: 0,
                                                excludeRecursion = extensions?.exclude_recursion
                                                    ?: false,
                                                preventRecursion = extensions?.prevent_recursion
                                                    ?: false,
                                                delayUntilRecursion = extensions?.delay_until_recursion
                                                    ?: false,
                                                group = extensions?.group ?: "",
                                                groupOverride = extensions?.group_override ?: false,
                                                groupWeight = extensions?.group_weight ?: 100,
                                                useGroupScoring = extensions?.use_group_scoring
                                                    ?: false,
                                                scanDepth = extensions?.scan_depth ?: 2,
                                                caseSensitive = extensions?.case_sensitive ?: false,
                                                automationId = extensions?.automation_id ?: "",
                                                vectorized = extensions?.vectorized ?: false
                                            )
                                        )
                                    }
                                _saveWorldBookFlag.value = true
                            } catch (e: Exception) {
                                Timber.tag("CharacterEditViewModel").e(e, "解析世界书失败")
                            }
                        }

                        ToastUtils.showToast("导入成功: ${cardV3.data.name}")
                    } catch (e: Exception) {
                        Timber.tag("CharacterEditViewModel").e(e, "解析角色卡失败")
//                        _currentCharacter.value = null
                        ToastUtils.showToast("解析失败，角色卡格式错误")
                    }
                } else {
//                    _currentCharacter.value = null
                    ToastUtils.showToast("解析失败，未找到角色数据")
                }
            }
        }
    }

    /**
     * 创建图片选择Intent
     */
    fun createImagePickerIntent(): Intent {
        return Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
    }

    fun onSaveRegexGroupChange(enabled: Boolean) {
        _saveRegexGroupFlag.value = enabled
    }

    fun onSaveWorldBookChange(enabled: Boolean) {
        _saveWorldBookFlag.value = enabled
    }

    /**
     * 保存角色：直接从内部 StateFlow 读取数据
     */
    fun saveCharacter() {
        val current = _currentCharacter.value ?: return
        if (current.name.isEmpty()) {
            ToastUtils.showLong("角色名称不能为空")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val charId = _characterId.value
            val convId = _conversationId.value

            // 1. 处理图片保存
            if (hasNewAvatar) {
                imageManager.deleteAvatarImage(charId)
                avatarBitmap?.let { imageManager.saveAvatarImage(charId, it) }
            }
            if (hasNewBackground) {
                imageManager.deleteBackgroundImage(charId)
                backgroundBitmap?.let { imageManager.saveBackgroundImage(charId, it) }
            }

            // 2. 更新实体的时间和路径
            val now = System.currentTimeMillis()
            val existing = aiCharacterRepository.getCharacterById(charId)
            val characterToSave = current.copy(
                id = charId,
                avatar = imageManager.getAvatarImagePath(charId),
                background = imageManager.getBackgroundImagePath(charId),
                creation_date = existing?.creation_date ?: now,
                modification_date = now
            )

            // 3. 执行数据库操作
            if (isNewCharacter.value) {
                aiCharacterRepository.insertAICharacter(characterToSave)
            } else {
                aiCharacterRepository.updateAICharacter(characterToSave)
            }

            // 4. 保存记忆
            val existingMemory = aiChatMemoryRepository.getByCharacterIdAndConversationId(charId, convId)
            if (existingMemory != null) {
                aiChatMemoryRepository.update(existingMemory.copy(content = _memory.value, count = _memoryCount.value))
            } else {
                aiChatMemoryRepository.insert(AIChatMemory(
                    id = NanoIdUtils.randomNanoId(),
                    characterId = charId,
                    conversationId = convId,
                    content = _memory.value,
                    createdAt = now,
                    count = _memoryCount.value
                ))
            }

            // 5. 处理正则组和世界书保存
            var finalEnabledRegexGroups: List<String>? = null
            if (saveRegexGroupFlag.value == true && parsedRegexGroup.value != null) {
                yiyiRegexGroupRepository.insertGroup(parsedRegexGroup.value!!)
                yiyiRegexScriptRepository.insertScripts(parsedRegexScripts.value)
                finalEnabledRegexGroups = listOf(parsedRegexGroup.value!!.id)
            }

            var finalChatWorldId = _chatWorldId.value
            if (saveWorldBookFlag.value == true && parsedWorldBook.value != null) {
                yiyiWorldBookRepository.saveBookWithEntries(
                    parsedWorldBook.value!!,
                    parsedWorldBookEntries.value
                )
                // 如果保存了导入的世界书，自动将其加入当前对话的已选世界书列表
                if (!finalChatWorldId.contains(parsedWorldBook.value!!.id)) {
                    finalChatWorldId = finalChatWorldId + parsedWorldBook.value!!.id
                }
            }

            // 6. 更新对话信息
            val conversation = conversationRepository.getById(convId)
            if (conversation != null) {
                var updatedRegexGroups = conversation.enabledRegexGroups ?: emptyList()
                if (finalEnabledRegexGroups != null) {
                    val newId = finalEnabledRegexGroups.first()
                    if (!updatedRegexGroups.contains(newId)) {
                        updatedRegexGroups = updatedRegexGroups + newId
                    }
                }
                conversationRepository.update(conversation.copy(
                    playerName = userConfigState.userConfig.value?.userName ?: "user9527",
                    chatWorldId = finalChatWorldId,
                    enabledRegexGroups = updatedRegexGroups
                ))
            } else {
                conversationRepository.insert(Conversation(
                    id = convId,
                    name = characterToSave.name,
                    type = ConversationType.SINGLE,
                    characterIds = mapOf(charId to 1.0f),
                    playerName = userConfigState.userConfig.value?.userName ?: "user9527",
                    avatarPath = characterToSave.avatar,
                    backgroundPath = characterToSave.background,
                    chatWorldId = finalChatWorldId,
                    characterKeywords = null,
                    playGender = "",
                    playerDescription = "",
                    chatSceneDescription = "",
                    additionalSummaryRequirement = "",
                    enabledRegexGroups = finalEnabledRegexGroups
                ))
            }

            withContext(Dispatchers.Main) {
                ToastUtils.showToast("角色保存成功")
                navigateBack()
            }
        }
    }
}
