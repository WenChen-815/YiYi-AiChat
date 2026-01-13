package com.wenchen.yiyi.feature.main.viewmodel

import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState
) : BaseViewModel(
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState
) {
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val conversationDao = Application.Companion.appDatabase.conversationDao()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            try {
                // 获取所有群组对话
                val allConversations = conversationDao.getAllConversations()
                _conversations.value = allConversations
            } catch (e: Exception) {
                // 错误处理
                _conversations.value = emptyList()
            }
        }
    }

    // 刷新群组列表
    fun refreshConversations() {
        loadConversations()
    }

    // 创建新的群组对话
    fun createConversation(conversation: Conversation) {
        viewModelScope.launch {
            try {
                conversationDao.insert(conversation)
                // 创建成功后重新加载列表
                loadConversations()
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    // 删除群组对话
    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            try {
                conversationDao.deleteById(conversation.id)
                // 删除成功后重新加载列表
                loadConversations()
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    // 更新群组对话
    fun updateConversation(conversation: Conversation) {
        viewModelScope.launch {
            try {
                conversationDao.update(conversation)
                // 更新成功后重新加载列表
                loadConversations()
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }
}