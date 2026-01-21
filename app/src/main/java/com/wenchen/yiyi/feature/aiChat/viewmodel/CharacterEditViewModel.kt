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
import com.wenchen.yiyi.feature.output.model.CharacterData
import com.wenchen.yiyi.feature.output.model.CharacterExtensions
import com.wenchen.yiyi.feature.output.model.RegexScript
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.decodeFromString
import javax.inject.Inject

@HiltViewModel
class CharacterEditViewModel @Inject constructor(
    private val aiCharacterRepository: AICharacterRepository,
    private val aiChatMemoryRepository: AIChatMemoryRepository,
    private val conversationRepository: ConversationRepository,
    private val yiyiRegexGroupRepository: YiYiRegexGroupRepository,
    private val yiyiRegexScriptRepository: YiYiRegexScriptRepository,
    private val yiyiWorldBookRepository: YiYiWorldBookRepository,
    private val yiyiWorldBookEntryRepository: YiYiWorldBookEntryRepository,
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    savedStateHandle: SavedStateHandle
) : BaseViewModel(
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState,
) {
    val _characterId = MutableStateFlow("")
    val characterId: StateFlow<String> = _characterId.asStateFlow()

    val _conversationId = MutableStateFlow("")
    var conversationId: StateFlow<String> = _conversationId.asStateFlow()

    val _isNewCharacter = MutableStateFlow(false)
    val isNewCharacter: StateFlow<Boolean> = _isNewCharacter.asStateFlow()

    // 解析测试相关的状态
    private val _parsedCharacter = MutableStateFlow<CharacterData?>(null)
    val parsedCharacter: StateFlow<CharacterData?> = _parsedCharacter.asStateFlow()

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

    private val _parsedMemory = MutableStateFlow<String>("")
    val parsedMemory: StateFlow<String?> = _parsedMemory.asStateFlow()

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
        } else {
            _characterId.value = route.characterId
        }
        _conversationId.value = "${userConfigState.userConfig.value?.userId}_${characterId.value}"
    }

    fun loadCharacterData(
        conversationId: String,
        characterId: String,
        onLoadComplete: (AICharacter?, AIChatMemory?) -> Unit
    ) {
        /*
        虽然loadCharacterData函数已经在viewModelScope.launch中启动了协程，
        但默认情况下viewModelScope可能使用的是主线程调度器，
        需要明确指定使用IO线程来执行数据库操作
        */
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val character = aiCharacterRepository.getCharacterById(characterId)
                val memory =
                    aiChatMemoryRepository.getByCharacterIdAndConversationId(
                        characterId,
                        conversationId
                    )
                Timber.tag("CharacterEditViewModel")
                    .i("加载角色记忆: $memory characterId:$characterId conversationId:$conversationId")
                onLoadComplete(character, memory)
            } catch (e: Exception) {
                Timber.tag("CharacterEditViewModel").e(e, "加载角色数据失败: $characterId")
                onLoadComplete(null, null)
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
                        _parsedCharacter.value = cardV3.data

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
                        _parsedMemory.value = extension?.memory ?: ""

                        // 尝试查找正则组
                        if (parsedCharacter.value?.extensions != null) {
                            try {
                                val regexExtension = AppJson.decodeFromString<CharacterExtensions>(
                                    parsedCharacter.value?.extensions.toString()
                                )
                                Timber.tag("CharacterEditViewModel").i("发现正则扩展！")
                                val regexGroupId = NanoIdUtils.randomNanoId()
                                _parsedRegexGroup.value = YiYiRegexGroup(
                                    id = regexGroupId,
                                    name = "${parsedCharacter.value?.name} 的正则",
                                    description = "${parsedCharacter.value?.name} 的正则",
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
                        if (parsedCharacter.value?.character_book != null) {
                            try {
                                val characterBook = parsedCharacter.value?.character_book
                                Timber.tag("CharacterEditViewModel").i("发现世界书！")
                                val worldBookId = NanoIdUtils.randomNanoId()
                                val bookName =
                                    characterBook?.name?.ifEmpty { "${parsedCharacter.value?.name} 的世界书" } ?: "${parsedCharacter.value?.name} 的世界书"
                                val bookDescription = characterBook?.description
                                    ?: "${parsedCharacter.value?.name} 的世界书"
                                _parsedWorldBook.value = YiYiWorldBook(
                                    id = worldBookId,
                                    name = bookName,
                                    description = bookDescription,
                                    createTime = System.currentTimeMillis(),
                                    updateTime = System.currentTimeMillis(),
                                )
                                _parsedWorldBookEntries.value =
                                    characterBook?.entries?.map { entry ->
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
                                                matchWholeWords = extensions?.match_whole_words ?: true,
                                                probability = extensions?.probability ?: 100,
                                                useProbability = extensions?.useProbability ?: true,
                                                sticky = extensions?.sticky ?: 0,
                                                cooldown = extensions?.cooldown ?: 0,
                                                delay = extensions?.delay ?: 0,
                                                excludeRecursion = extensions?.exclude_recursion ?: false,
                                                preventRecursion = extensions?.prevent_recursion ?: false,
                                                delayUntilRecursion = extensions?.delay_until_recursion ?: false,
                                                group = extensions?.group ?: "",
                                                groupOverride = extensions?.group_override ?: false,
                                                groupWeight = extensions?.group_weight ?: 100,
                                                useGroupScoring = extensions?.use_group_scoring ?: false,
                                                scanDepth = extensions?.scan_depth ?: 2,
                                                caseSensitive = extensions?.case_sensitive ?: false,
                                                automationId = extensions?.automation_id ?: "",
                                                vectorized = extensions?.vectorized ?: false
                                            )
                                        )
                                    } ?: emptyList()
                                _saveWorldBookFlag.value = true
                            } catch (e: Exception) {
                                Timber.tag("CharacterEditViewModel").e(e, "解析世界书失败")
                            }
                        }

                        ToastUtils.showToast("导入成功: ${cardV3.data.name}")
                    } catch (e: Exception) {
                        Timber.tag("CharacterEditViewModel").e(e, "解析角色卡失败")
                        _parsedCharacter.value = null
                        ToastUtils.showToast("解析失败，角色卡格式错误")
                    }
                } else {
                    _parsedCharacter.value = null
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

    // 保存角色数据
    fun saveCharacter(
        name: String,
        description: String,
        first_mes: String?,
        mes_example: String?,
        personality: String?,
        scenario: String?,
        creator_notes: String?,
        system_prompt: String?,
        post_history_instructions: String?,
        memory: String,
        memoryCount: Int,
        playerName: String,
        playGender: String,
        playerDescription: String,
        chatWorldId: List<String>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isEmpty()) {
                ToastUtils.showLong("角色名称不能为空")
                return@launch
            }

            // 清理并保存新图片
            if (hasNewAvatar) {
                imageManager.deleteAvatarImage(characterId.value)
                imageManager.saveAvatarImage(characterId.value, avatarBitmap!!)
            }
            if (hasNewBackground) {
                imageManager.deleteBackgroundImage(characterId.value)
                imageManager.saveBackgroundImage(characterId.value, backgroundBitmap!!)
            }

            // 获取现有角色以保留创建日期，如果是新角色则使用当前时间
            val existingCharacter = aiCharacterRepository.getCharacterById(characterId.value)
            val now = System.currentTimeMillis()

            val character = AICharacter(
                id = characterId.value,
                name = name,
                userId = userState.userId.value,
                description = description,
                first_mes = first_mes,
                mes_example = mes_example,
                personality = personality,
                scenario = scenario,
                creator_notes = creator_notes,
                system_prompt = system_prompt,
                post_history_instructions = post_history_instructions,
                avatar = imageManager.getAvatarImagePath(characterId.value),
                background = imageManager.getBackgroundImagePath(characterId.value),
                creation_date = existingCharacter?.creation_date ?: now,
                modification_date = now
            )

            if (isNewCharacter.value) {
                aiCharacterRepository.insertAICharacter(character)
            } else {
                aiCharacterRepository.updateAICharacter(character)
            }

            // 处理角色记忆
            val existingMemory = aiChatMemoryRepository.getByCharacterIdAndConversationId(
                characterId.value,
                conversationId.value
            )
            if (existingMemory != null) {
                aiChatMemoryRepository.update(
                    existingMemory.copy(
                        content = memory,
                        count = memoryCount
                    )
                )
            } else {
                aiChatMemoryRepository.insert(
                    AIChatMemory(
                        id = NanoIdUtils.randomNanoId(),
                        characterId = characterId.value,
                        conversationId = conversationId.value,
                        content = memory.ifEmpty { "" },
                        createdAt = System.currentTimeMillis(),
                        count = memoryCount
                    )
                )
            }

            // 处理正则组保存
            var finalEnabledRegexGroups: List<String>? = null
            if (saveRegexGroupFlag.value == true && parsedRegexGroup.value != null) {
                yiyiRegexGroupRepository.insertGroup(parsedRegexGroup.value!!)
                yiyiRegexScriptRepository.insertScripts(parsedRegexScripts.value)
                finalEnabledRegexGroups = listOf(parsedRegexGroup.value!!.id)
            }

            var finalChatWorldId = chatWorldId
            if (saveWorldBookFlag.value == true && parsedWorldBook.value != null) {
                yiyiWorldBookRepository.saveBookWithEntries(parsedWorldBook.value!!, parsedWorldBookEntries.value)
                // 如果保存了导入的世界书，自动将其加入当前对话的已选世界书列表
                if (!finalChatWorldId.contains(parsedWorldBook.value!!.id)) {
                    finalChatWorldId = finalChatWorldId + parsedWorldBook.value!!.id
                }
            }

            // 更新对话的世界书 ID 和正则组 ID
            val conversation = conversationRepository.getById(conversationId.value)
            if (conversation != null) {
                var updatedRegexGroups = conversation.enabledRegexGroups ?: emptyList()
                if (finalEnabledRegexGroups != null) {
                    val newId = finalEnabledRegexGroups.first()
                    if (!updatedRegexGroups.contains(newId)) {
                        updatedRegexGroups = updatedRegexGroups + newId
                    }
                }
                conversationRepository.update(
                    conversation.copy(
                        chatWorldId = finalChatWorldId,
                        enabledRegexGroups = updatedRegexGroups
                    )
                )
            } else {
                val newConversation = Conversation(
                    id = conversationId.value,
                    name = name,
                    type = ConversationType.SINGLE,
                    characterIds = mapOf(characterId.value to 1.0f),
                    characterKeywords = mapOf(characterId.value to emptyList()),
                    playerName = playerName,
                    playGender = playGender,
                    playerDescription = playerDescription,
                    chatSceneDescription = "",
                    additionalSummaryRequirement = "",
                    avatarPath = character.avatar,
                    backgroundPath = character.background,
                    chatWorldId = finalChatWorldId,
                    enabledRegexGroups = finalEnabledRegexGroups
                )
                conversationRepository.insert(newConversation)
            }

            withContext(Dispatchers.Main) {
                ToastUtils.showToast("角色保存成功")
                navigateBack()
            }
        }
    }
}
