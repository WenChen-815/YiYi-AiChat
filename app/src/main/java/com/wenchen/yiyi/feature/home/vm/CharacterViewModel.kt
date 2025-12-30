package com.wenchen.yiyi.feature.home.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.App
import com.wenchen.yiyi.core.common.entity.AICharacter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CharacterViewModel : ViewModel() {
    private val _characters = MutableStateFlow<List<AICharacter>>(emptyList())
    val characters: StateFlow<List<AICharacter>> = _characters

    private val _query = MutableStateFlow<String>("")
    val query: StateFlow<String> = _query

    private val aiCharacterDao = App.appDatabase.aiCharacterDao()

    init {
        loadCharacters()
    }

    fun loadCharacters() {
        viewModelScope.launch {
            try {
                // 获取所有角色
                val allCharacters = aiCharacterDao.getAllCharacters()
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
                    val allCharacters = aiCharacterDao.getAllCharacters()
                    _characters.value = allCharacters
                } else {
                    // 根据查询过滤角色
                    val filteredCharacters = aiCharacterDao.getCharacterByName(newQuery)
                    _characters.value = filteredCharacters
                }
            } catch (e: Exception) {
                _characters.value = emptyList()
            }
        }
    }

    fun deleteCharacter(character: AICharacter) {
        Log.d("CharacterViewModel2", "尝试删除角色: ${character.name}")
        viewModelScope.launch {
            try {
                // 从数据库删除角色
                Log.d("CharacterViewModel2", "onLaunch删除角色: ${character.name}")
                val result = withContext(Dispatchers.IO) {
                    aiCharacterDao.deleteAICharacter(character)
                }
                if (result > 0) {
                    // 删除成功，重新加载列表
                    Log.d("CharacterViewModel2", "onLaunch删除角色成功: ${character.name}")
                    searchCharacters(_query.value)
                }
            } catch (e: Exception) {
                // 错误处理
                Log.e("CharacterViewModel2", "删除角色失败: ${character.name}", e)
            }
        }
    }

    companion object {
    }
}
