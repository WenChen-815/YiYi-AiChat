package com.wenchen.yiyi.feature.aiChat.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.App
import com.wenchen.yiyi.core.common.entity.AICharacter
import com.wenchen.yiyi.feature.aiChat.entity.AIChatMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CharacterEditViewModel : ViewModel() {
    private val aiCharacterDao = App.appDatabase.aiCharacterDao()
    private val aiChatMemoryDao = App.appDatabase.aiChatMemoryDao()

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
                val character = aiCharacterDao.getCharacterById(characterId)
                val memory =
                    aiChatMemoryDao.getByCharacterIdAndConversationId(characterId, conversationId)
                Log.i(
                    "CharacterEditViewModel",
                    "加载角色记忆: $memory characterId:$characterId conversationId:$conversationId"
                )
                onLoadComplete(character, memory)
            } catch (e: Exception) {
                Log.e("CharacterEditViewModel", "加载角色数据失败: $characterId", e)
                onLoadComplete(null, null)
            }
        }
    }
}
