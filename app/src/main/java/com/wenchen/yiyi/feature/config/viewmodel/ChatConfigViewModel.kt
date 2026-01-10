package com.wenchen.yiyi.feature.config.viewmodel

import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.core.base.state.BaseNetWorkUiState
import com.wenchen.yiyi.core.base.viewmodel.BaseNetWorkViewModel
import com.wenchen.yiyi.core.model.network.Model
import com.wenchen.yiyi.core.model.network.ModelsResponse
import com.wenchen.yiyi.core.data.repository.AiHubRepository
import com.wenchen.yiyi.core.model.config.UserConfig
import com.wenchen.yiyi.core.model.network.NetworkResponse
import com.wenchen.yiyi.core.result.ResultHandler
import com.wenchen.yiyi.core.result.asResult
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.util.toast.ToastUtils
import com.wenchen.yiyi.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatConfigViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    private val userConfigState: UserConfigState,
    private val aiHubRepository: AiHubRepository
) : BaseNetWorkViewModel<ModelsResponse>(
    navigator = navigator,
    userState = userState
){
    val userConfig = userConfigState.userConfig.value
    private val _models = MutableStateFlow<List<Model>>(emptyList())
    val models: StateFlow<List<Model>> = _models.asStateFlow()

    private val _imgModels = MutableStateFlow<List<Model>>(emptyList())
    val imgModels: StateFlow<List<Model>> = _imgModels.asStateFlow()

    override fun requestApiFlow(): Flow<NetworkResponse<ModelsResponse>> {
        return aiHubRepository.getModels()
    }
    fun executeGetModels(
        apiKey: String,
        baseUrl: String,
        type: Int,
        setLoading: (Boolean) -> Unit,
        setSelectedModel: (String) -> Unit
    ) {
        ResultHandler.handleResultWithData(
            scope = viewModelScope,
            flow = aiHubRepository.getModels(baseUrl, apiKey).asResult(),
            onData = { modelsResponse ->
                setLoading(false)
                _uiState.value = BaseNetWorkUiState.Success(modelsResponse)
                // 默认选中第一个模型
                val defaultModel = modelsResponse.data.firstOrNull()?.id ?: "未选择模型"

                when(type) {
                    0 -> {
                        _models.value = modelsResponse.data.sortedBy { it.id }
                        val position = models.value.indexOfFirst { it.id == userConfig?.selectedModel }
                        setSelectedModel(models.value.getOrNull(position)?.id ?: defaultModel)
                    }
                    1 -> {
                        _imgModels.value = modelsResponse.data.sortedBy { it.id }
                        val position = imgModels.value.indexOfFirst { it.id == userConfig?.selectedImgModel }
                        setSelectedModel(imgModels.value.getOrNull(position)?.id ?: defaultModel)
                    }
                }
                ToastUtils.showToast("成功获取${modelsResponse.data.size}个模型")
            },
            onError = { message, exception ->
                Timber.tag("ChatConfigViewModel").d("Error: $message Exception: $exception")
                // 更新 uiState 为错误状态
                _uiState.value = BaseNetWorkUiState.Error(message, exception)
                setLoading(false)
                ToastUtils.showToast( message)
            },
            onLoading = {
                // 更新 uiState 为加载状态
                _uiState.value = BaseNetWorkUiState.Loading
                setLoading(true)
            }
        )
    }
    fun updateUserConfig(userConfig: UserConfig) {
        viewModelScope.launch {
            userConfigState.updateUserConfig(userConfig)
        }
    }
}