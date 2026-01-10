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
import com.wenchen.yiyi.core.util.toast.ToastUtils
import com.wenchen.yiyi.core.datastore.storage.ImageManager
import com.wenchen.yiyi.core.database.entity.AIChatMemory
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.core.util.BitMapUtil
import com.wenchen.yiyi.feature.output.model.OutputCharacterModel
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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CharacterEditViewModel @Inject constructor(
    private val aiCharacterRepository: AICharacterRepository,
    private val aiChatMemoryRepository: AIChatMemoryRepository,
    private val conversationRepository: ConversationRepository,
    navigator: AppNavigator,
    userState: UserState,
    private val userConfigState: UserConfigState,
    savedStateHandle: SavedStateHandle
) : BaseViewModel(
    navigator = navigator,
    userState = userState
) {
    val _characterId = MutableStateFlow("")
    val characterId: StateFlow<String> = _characterId.asStateFlow()

    val _conversationId = MutableStateFlow("")
    var conversationId: StateFlow<String> = _conversationId.asStateFlow()

    val _isNewCharacter = MutableStateFlow(false)
    val isNewCharacter: StateFlow<Boolean> = _isNewCharacter.asStateFlow()

    // 解析测试相关的状态
    private val _parsedCharacter = MutableStateFlow<OutputCharacterModel?>(null)
    val parsedCharacter: StateFlow<OutputCharacterModel?> = _parsedCharacter.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()
    
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
            _characterId.value = UUID.randomUUID().toString()
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
                    aiChatMemoryRepository.getByCharacterIdAndConversationId(characterId, conversationId)
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
            val result = BitMapUtil.readLargeDataFromUri<OutputCharacterModel>(context, uri)
            withContext(Dispatchers.Main) {
                if (result != null) {
                    _parsedCharacter.value = result
                    // 更新图片状态
                    result.avatarByte?.let {
                        avatarBitmap = BitMapUtil.byteArrayToBitmap(it)
                        hasNewAvatar = true
                    }
                    result.backgroundByte?.let {
                        backgroundBitmap = BitMapUtil.byteArrayToBitmap(it)
                        hasNewBackground = true
                    }
                    ToastUtils.showToast("导入成功: ${result.name}")
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

    // 保存角色数据
    fun saveCharacter(
        name: String,
        roleIdentity: String,
        roleAppearance: String,
        roleDescription: String,
        outputExample: String,
        behaviorRules: String,
        memory: String,
        memoryCount: Int,
        playerName: String,
        playGender: String,
        playerDescription: String,
        chatWorldId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isEmpty()) {
                ToastUtils.showLong("角色名称不能为空")
                Timber.tag("CharacterEditViewModel").d("角色名称不能为空")
                return@launch
            }

            // 清理旧图片（如果有新图片被选择）
            if (hasNewAvatar) {
                imageManager.deleteAvatarImage(characterId.value)
                imageManager.saveAvatarImage(characterId.value, avatarBitmap!!)
            }
            if (hasNewBackground) {
                imageManager.deleteBackgroundImage(characterId.value)
                imageManager.saveBackgroundImage(characterId.value, backgroundBitmap!!)
            }

            val character = AICharacter(
                aiCharacterId = characterId.value,
                name = name,
                roleIdentity = roleIdentity,
                roleAppearance = roleAppearance,
                roleDescription = roleDescription,
                outputExample = outputExample,
                behaviorRules = behaviorRules,
                userId = userState.userId.value,
                createdAt = System.currentTimeMillis(),
                avatarPath = imageManager.getAvatarImagePath(characterId.value),
                backgroundPath = imageManager.getBackgroundImagePath(characterId.value)
            )

            var memoryEntity = aiChatMemoryRepository.getByCharacterIdAndConversationId(
                character.aiCharacterId,
                conversationId.value
            )
            if (memoryEntity == null) {
                memoryEntity = AIChatMemory(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId.value,
                    characterId = character.aiCharacterId,
                    content = memory.ifEmpty { "" },
                    createdAt = System.currentTimeMillis(),
                    count = memoryCount
                )
            } else {
                memoryEntity.content = memory.ifEmpty { "" }
                memoryEntity.count = memoryCount
            }
            Timber.tag("CharacterEditViewModel").d("保存角色数据")
            try {
                if (isNewCharacter.value) {
                    // 新增角色
                    val result = aiCharacterRepository.insertAICharacter(character)
                    val result2 = aiChatMemoryRepository.insert(memoryEntity)

                    // 创建初始Conversation
                    val initialConversation = Conversation(
                        id = conversationId.value,
                        name = character.name,
                        type = ConversationType.SINGLE,
                        characterIds = mapOf(character.aiCharacterId to 1.0f),
                        characterKeywords = mapOf(character.aiCharacterId to emptyList()),
                        playerName = playerName.ifEmpty {
                            userConfigState.userConfig.value?.userName ?: ""
                        },
                        playGender = playGender,
                        playerDescription = playerDescription,
                        chatWorldId = chatWorldId,
                        chatSceneDescription = "",
                        additionalSummaryRequirement = "",
                        avatarPath = character.avatarPath,
                        backgroundPath = character.backgroundPath,
                    )
                    conversationRepository.insert(initialConversation) // 保存初始对话

                    withContext(Dispatchers.Main) {
                        if (result > 0 && result2 > 0) {
                            ToastUtils.show("保存成功")
                        } else {
                            ToastUtils.show("保存失败")
                        }
                        navigateBack()
                    }
                } else {
                    // 更新角色
                    val result = aiCharacterRepository.updateAICharacter(character)
                    val result2 = aiChatMemoryRepository.update(memoryEntity)
                    withContext(Dispatchers.Main) {
                        if (result > 0 && result2 > 0) {
                            ToastUtils.show("更新成功")
                        } else {
                            ToastUtils.show("更新失败")
                        }
                        navigateBack()
                    }
                }
            } catch (e: Exception) {
                Timber.tag("CharacterEditViewModel").d("保存角色数据失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    ToastUtils.show("保存角色数据失败: ${e.message}")
                }
            }
        }
    }
}
