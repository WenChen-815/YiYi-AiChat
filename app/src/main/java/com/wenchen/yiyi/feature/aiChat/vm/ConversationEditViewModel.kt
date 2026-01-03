package com.wenchen.yiyi.feature.aiChat.vm

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.squareup.moshi.JsonAdapter
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.common.utils.FilesUtil
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.util.toast.ToastUtils
import com.wenchen.yiyi.feature.aiChat.common.ImageManager
import com.wenchen.yiyi.feature.aiChat.entity.Conversation
import com.wenchen.yiyi.feature.aiChat.entity.ConversationType
import com.wenchen.yiyi.feature.worldBook.entity.WorldBook
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
class ConversationEditViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    savedStateHandle: SavedStateHandle
) : BaseViewModel(
    navigator = navigator,
    userState = userState
) {
    val _conversationId = MutableStateFlow("")
    var conversationId: StateFlow<String> = _conversationId.asStateFlow()

    val _isNewConversation = MutableStateFlow(false)
    val isNewConversation: StateFlow<Boolean> = _isNewConversation.asStateFlow()

    private val conversationDao = Application.appDatabase.conversationDao()
    private val imageManager = ImageManager()

    var avatarBitmap by mutableStateOf<Bitmap?>(null)
    var backgroundBitmap by mutableStateOf<Bitmap?>(null)
    var hasNewAvatar by mutableStateOf(false)
    var hasNewBackground by mutableStateOf(false)

    init {
        val route = savedStateHandle.toRoute<AiChatRoutes.ConversationEdit>()
        _isNewConversation.value = route.isNewConversation
        if (isNewConversation.value) {
            _conversationId.value = UUID.randomUUID().toString()
        } else {
            _conversationId.value = route.conversationId
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
     * 创建图片选择Intent
     */
    fun createImagePickerIntent(): Intent {
        return Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
    }

    fun saveConversation(
        name: String,
        playerName: String,
        playGender: String,
        playerDescription: String,
        chatWorldId: String,
        chatSceneDescription: String,
        additionalSummaryRequirement: String,
        chooseCharacterMap: Map<String, Float>,
        characterKeywordsMap: Map<String, List<String>>,
    ) {
        viewModelScope.launch {
            if (name.isEmpty()) {
                ToastUtils.showToast("请输入对话名称")
                return@launch
            }
            try {
                // 清理旧图片（如果有新图片被选择）
                if (hasNewAvatar) {
                    imageManager.deleteAvatarImage(conversationId.value)
                    imageManager.saveAvatarImage(conversationId.value, avatarBitmap!!)
                }
                if (hasNewBackground) {
                    imageManager.deleteBackgroundImage(conversationId.value)
                    imageManager.saveBackgroundImage(conversationId.value, backgroundBitmap!!)
                }

                val existingConversation = conversationDao.getById(conversationId.value)
                if (existingConversation != null) {
                    if (hasNewAvatar) {
                        imageManager.deleteAvatarImage(conversationId.value)
                        imageManager.saveAvatarImage(conversationId.value, avatarBitmap!!)
                    }
                    if (hasNewBackground) {
                        imageManager.deleteBackgroundImage(conversationId.value)
                        imageManager.saveBackgroundImage(conversationId.value, backgroundBitmap!!)
                    }
                    val updatedConversation =
                        existingConversation.copy(
                            name = name,
                            playerName = playerName,
                            playGender = playGender,
                            playerDescription = playerDescription,
                            chatWorldId = chatWorldId,
                            chatSceneDescription = chatSceneDescription,
                            additionalSummaryRequirement = additionalSummaryRequirement,
                            characterIds = chooseCharacterMap,
                            characterKeywords = characterKeywordsMap,
                            avatarPath =
                                if (hasNewAvatar) {
                                    imageManager.getAvatarImagePath(
                                        conversationId.value,
                                    )
                                } else {
                                    existingConversation.avatarPath
                                },
                            backgroundPath =
                                if (hasNewBackground) {
                                    imageManager.getBackgroundImagePath(
                                        conversationId.value,
                                    )
                                } else {
                                    existingConversation.backgroundPath
                                },
                        )

                    val result = conversationDao.update(updatedConversation)

                    if (result > 0) {
                        ToastUtils.showToast("更新成功")
                        navigateBack()
                    } else {
                        ToastUtils.showToast("更新失败")
                    }
                } else {
                    // 创建新对话的情况
                    val newConversation =
                        Conversation(
                            id = conversationId.value,
                            name = name,
                            type = ConversationType.GROUP,
                            characterIds = chooseCharacterMap,
                            characterKeywords = characterKeywordsMap,
                            playerName = playerName,
                            playGender = playGender,
                            playerDescription = playerDescription,
                            chatWorldId = chatWorldId,
                            chatSceneDescription = chatSceneDescription,
                            additionalSummaryRequirement = additionalSummaryRequirement,
                            avatarPath =
                                if (hasNewAvatar) {
                                    imageManager.getAvatarImagePath(
                                        conversationId.value,
                                    )
                                } else {
                                    ""
                                },
                            backgroundPath =
                                if (hasNewBackground) {
                                    imageManager.getBackgroundImagePath(
                                        conversationId.value,
                                    )
                                } else {
                                    ""
                                },
                        )

                    conversationDao.insert(newConversation)

                    ToastUtils.showToast("创建成功")
                    navigateBack()
                }
            } catch (e: Exception) {
                Timber.e(e, "保存对话失败")
                ToastUtils.showToast("保存对话失败")
            }
        }
    }

    // 从文件加载世界书列表
    fun loadWorldBooks(
        adapter: JsonAdapter<WorldBook>,
        onLoaded: (List<WorldBook>) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val worldBookFiles = FilesUtil.listFileNames("world_book")
        Timber.tag("WorldBookListActivity").d("loadWorldBooks: $worldBookFiles")
        val worldBooks = mutableListOf(WorldBook("","未选择世界"))

        worldBookFiles.forEach { fileName ->
            try {
                val json = FilesUtil.readFile("world_book/$fileName")
                val worldBook = adapter.fromJson(json)
                worldBook?.let { worldBooks.add(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onLoaded(worldBooks)
    }
}