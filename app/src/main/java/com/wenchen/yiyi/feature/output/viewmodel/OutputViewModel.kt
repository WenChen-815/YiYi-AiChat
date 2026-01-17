package com.wenchen.yiyi.feature.output.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.data.repository.AICharacterRepository
import com.wenchen.yiyi.core.data.repository.AIChatMemoryRepository
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.util.BitMapUtil
import com.wenchen.yiyi.core.util.CharaCardParser
import com.wenchen.yiyi.core.util.toast.ToastUtils
import com.wenchen.yiyi.feature.output.component.OutputCharacterView
import com.wenchen.yiyi.feature.output.model.CharaCardV3
import com.wenchen.yiyi.feature.output.model.CharaExtensionModel
import com.wenchen.yiyi.feature.output.model.CharacterData
import com.wenchen.yiyi.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OutputViewModel @Inject constructor(
    private val aiCharacterRepository: AICharacterRepository,
    private val memoryRepository: AIChatMemoryRepository,
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
) : BaseViewModel(
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState
) {
    private val _characters = MutableStateFlow<List<AICharacter>>(emptyList())
    val characters: StateFlow<List<AICharacter>> = _characters.asStateFlow()

    private val _selectedIndex = MutableStateFlow<List<Int>>(emptyList())
    val selectedIndex: StateFlow<List<Int>> =
        _selectedIndex.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    init {
        loadCharacters()
    }

    private fun loadCharacters() {
        viewModelScope.launch {
            try {
                // 获取所有角色
                val allCharacters = aiCharacterRepository.getAllCharacters()
                _characters.value = allCharacters
            } catch (e: Exception) {
                // 错误处理
                _characters.value = emptyList()
            }
        }
    }

    fun onItemClick(index: Int) {
        val currentSelected = _selectedIndex.value.toMutableList()
        if (currentSelected.contains(index)) {
            // 如果已存在，则移除，实现取消选择
            currentSelected.remove(index)
        } else {
            // 如果不存在，则添加，实现选择
            currentSelected.add(index)
        }
        _selectedIndex.value = currentSelected
    }

    fun outputSelected(context: Context) {
        _isExporting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            if (_selectedIndex.value.isEmpty()) return@launch
            selectedIndex.value.forEach { index ->
                val character = characters.value[index]
                // 获取角色记忆
                val memory = memoryRepository.getByCharacterIdAndConversationId(
                    character.id,
                    conversationId = "${userState.userId.value}_${character.id}",
                )
                // 加载角色头像 (IO线程)
                val avatar = BitMapUtil.loadBitmapFromUri(context, character.avatar?.toUri(), false)
                val background = BitMapUtil.loadBitmapFromUri(context, character.background?.toUri(), false)
                
                // 转换头像和背景为 ByteArray
                val avatarByte = avatar?.let {
                    BitMapUtil.bitmapToByteArray(it, Bitmap.CompressFormat.PNG)
                }
                val backgroundByte = background?.let {
                    BitMapUtil.bitmapToByteArray(it, Bitmap.CompressFormat.PNG)
                }

                // 构建符合 CharaCardV3 格式的对象，并将头像和背景存入特定字段
                val cardV3 = CharaCardV3(
                    data = CharacterData(
                        name = character.name,
                        description = character.description,
                        personality = character.personality ?: "",
                        scenario = character.scenario ?: "",
                        first_mes = character.first_mes ?: "",
                        mes_example = character.mes_example ?: "",
                        creator_notes = character.creator_notes ?: "",
                        system_prompt = character.system_prompt ?: "",
                        post_history_instructions = character.post_history_instructions ?: "",
                        creation_date = character.creation_date,
                        modification_date = character.modification_date,
                    )
                )

                // YiYi 扩展数据 (追加到文件末尾)
                val extension = CharaExtensionModel(
                    avatarByte = avatarByte,
                    backgroundByte = backgroundByte,
                    memory = memory?.content
                )

                /*
                 截图必须在主线程执行 (Main线程)
                 这里不会导致后续保存文件的bitmap为空
                 由于协程的机制，withContext 是一个挂起（suspend）操作，它会保证代码的顺序执行
                 */
                val bitmap = withContext(Dispatchers.Main) {
                    BitMapUtil.captureComposableAsBitmap(
                        context = context,
                        content = {
                            OutputCharacterView(
                                character, avatar, background
                            )
                        },
                        width = 540,
                    )
                }

                // 保存文件，直接传入 CharaCardV3 对象
                CharaCardParser.saveBitmapWithTavernCardAndLargeDataToGallery(
                    context, bitmap, "${character.name}_${System.currentTimeMillis()}", cardV3, extension
                )
            }
            withContext(Dispatchers.Main) {
                ToastUtils.showToast("已保存，请到相册查看")
                _isExporting.value = false
                _selectedIndex.value = emptyList()
            }
        }
    }
}
