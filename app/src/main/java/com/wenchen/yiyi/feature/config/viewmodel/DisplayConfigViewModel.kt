package com.wenchen.yiyi.feature.config.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.model.config.UserConfig
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.feature.aiChat.common.ChatLayoutMode
import com.wenchen.yiyi.feature.aiChat.common.LayoutType
import com.wenchen.yiyi.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisplayConfigViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
) : BaseViewModel(navigator, userState, userConfigState) {

    // 观察用户配置状态
    val userConfig: StateFlow<UserConfig?> = userConfigState.userConfig

    /**
     * 更新布局相关配置
     * 使用可选参数，仅更新传入的非空值
     */
    fun updateLayoutMode(
        mode: LayoutType? = null,
        userBubbleColor: Color? = null,
        userTextColor: Color? = null,
        aiBubbleColor: Color? = null,
        aiTextColor: Color? = null
    ) {
        viewModelScope.launch {
            val currentConfig = userConfig.value ?: return@launch
            val currentLayoutMode = currentConfig.layoutMode
            
            // 创建新的布局配置对象
            val newLayoutMode = currentLayoutMode.copy(
                type = mode ?: currentLayoutMode.type,
                userBubbleColor = userBubbleColor ?: currentLayoutMode.userBubbleColor,
                userTextColor = userTextColor ?: currentLayoutMode.userTextColor,
                aiBubbleColor = aiBubbleColor ?: currentLayoutMode.aiBubbleColor,
                aiTextColor = aiTextColor ?: currentLayoutMode.aiTextColor
            )
            
            // 更新全局配置
            val newUserConfig = currentConfig.copy(layoutMode = newLayoutMode)
            userConfigState.updateUserConfig(newUserConfig)
        }
    }

    /**
     * 恢复默认显示设置
     */
    fun restoreDefault() {
        viewModelScope.launch {
            val currentConfig = userConfig.value ?: return@launch
            // 将 layoutMode 重置为初始默认值
            val newUserConfig = currentConfig.copy(layoutMode = ChatLayoutMode())
            userConfigState.updateUserConfig(newUserConfig)
        }
    }
}
