package com.wenchen.yiyi.core.state

import com.wenchen.yiyi.core.data.repository.UserConfigStoreRepository
import com.wenchen.yiyi.core.model.config.UserConfig
import com.wenchen.yiyi.core.state.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserConfigState @Inject constructor(
    private val userConfigStoreRepository: UserConfigStoreRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope
){
    // 允许基础使用
    private val _allowBasicUsage = MutableStateFlow(false)
    val allowBasicUsage : StateFlow<Boolean> = _allowBasicUsage.asStateFlow()
    // 用户配置
    private val _userConfig = MutableStateFlow<UserConfig?>(null)
    val userConfig : StateFlow<UserConfig?> = _userConfig.asStateFlow()

    /**
     * 初始化应用状态
     * 由外部调用而不是自动初始化
     */
    fun initialize(initComplete : () -> Unit = {}) {
        applicationScope.launch {
            initializeState()
            initComplete()
        }
    }

    /**
     * 从本地存储初始化
     */
    private suspend fun initializeState() {
        val userConfig = userConfigStoreRepository.getUserConfig()
        _userConfig.value = userConfig
        if (userConfig != null &&
            userConfig.userName?.isNotEmpty() == true &&
            userConfig.baseUrl?.isNotEmpty() == true &&
            userConfig.baseApiKey?.isNotEmpty() == true &&
            userConfig.selectedModel?.isNotEmpty() == true)
        {
            _allowBasicUsage.value = true
        }
    }

    /**
     * 更新用户配置
     */
    suspend fun updateUserConfig(userConfig: UserConfig) {
        // 保存到本地存储
        userConfigStoreRepository.saveUserConfig(userConfig)

        // 更新内存中的状态
        _userConfig.value = userConfig
    }
}