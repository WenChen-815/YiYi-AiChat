package com.wenchen.yiyi.feature.main.viewmodel

import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.data.repository.AICharacterRepository
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val aiCharacterRepository: AICharacterRepository,
    navigator: AppNavigator,
    userState: UserState
) : BaseViewModel(
    navigator = navigator,
    userState = userState
) {
    private val _characters = MutableStateFlow<List<AICharacter>>(emptyList())
    val characters: StateFlow<List<AICharacter>> = _characters

    private val _query = MutableStateFlow<String>("")
    val query: StateFlow<String> = _query

    init {
        loadCharacters()
    }

    fun loadCharacters() {
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

    // 重新查询所有角色
    fun refreshCharacters() {
        searchCharacters("")
    }

    fun searchCharacters(newQuery: String) {
        _query.value = newQuery
        viewModelScope.launch {
            try {
                if (newQuery.isEmpty()) {
                    // 如果查询为空，加载所有角色
                    val allCharacters = aiCharacterRepository.getAllCharacters()
                    _characters.value = allCharacters
                } else {
                    // 根据查询过滤角色
                    val filteredCharacters = aiCharacterRepository.getCharacterByName(newQuery)
                    _characters.value = filteredCharacters
                }
            } catch (e: Exception) {
                _characters.value = emptyList()
            }
        }
    }

    fun deleteCharacter(character: AICharacter) {
        Timber.tag("HomeViewModel").d("尝试删除角色: ${character.name}")
        viewModelScope.launch {
            try {
                // 从数据库删除角色
                Timber.tag("HomeViewModel").d("onLaunch删除角色: ${character.name}")
                val result = withContext(Dispatchers.IO) {
                    aiCharacterRepository.deleteAICharacter(character)
                }
                if (result > 0) {
                    // 删除成功，重新加载列表
                    Timber.tag("HomeViewModel").d("onLaunch删除角色成功: ${character.name}")
                    searchCharacters(_query.value)
                }
            } catch (e: Exception) {
                // 错误处理
                Timber.tag("HomeViewModel").e(e, "删除角色失败: ${character.name}")
            }
        }
    }
}
