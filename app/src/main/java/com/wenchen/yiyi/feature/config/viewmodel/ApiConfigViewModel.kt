package com.wenchen.yiyi.feature.config.viewmodel

import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.core.base.state.BaseNetWorkUiState
import com.wenchen.yiyi.core.base.viewmodel.BaseNetWorkViewModel
import com.wenchen.yiyi.core.data.repository.AiHubRepository
import com.wenchen.yiyi.core.model.config.ApiConfig
import com.wenchen.yiyi.core.model.network.Model
import com.wenchen.yiyi.core.model.network.ModelsResponse
import com.wenchen.yiyi.core.model.network.NetworkResponse
import com.wenchen.yiyi.core.result.ResultHandler
import com.wenchen.yiyi.core.result.asResult
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.util.ui.ToastUtils
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
class ApiConfigViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    private val aiHubRepository: AiHubRepository
) : BaseNetWorkViewModel<ModelsResponse>(
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState
) {
    val userConfig = userConfigState.userConfig.value
    
    private val _models = MutableStateFlow<List<Model>>(emptyList())
    val models: StateFlow<List<Model>> = _models.asStateFlow()

    private val _imgModels = MutableStateFlow<List<Model>>(emptyList())
    val imgModels: StateFlow<List<Model>> = _imgModels.asStateFlow()

    private val _apiUIState = MutableStateFlow<ApiConfigUIState>(ApiConfigUIState())
    val apiUIState: StateFlow<ApiConfigUIState> = _apiUIState.asStateFlow()

    override fun requestApiFlow(): Flow<NetworkResponse<ModelsResponse>> {
        return aiHubRepository.getModels()
    }

    fun executeGetModels(
        apiKey: String,
        baseUrl: String,
        type: Int,
        onModelSelected: (String) -> Unit
    ) {
        if (apiKey.isBlank() || baseUrl.isBlank()) {
            ToastUtils.showToast("请先填写API Key和中转地址")
            return
        }

        ResultHandler.handleResultWithData(
            scope = viewModelScope,
            flow = aiHubRepository.getModels(baseUrl, apiKey).asResult(),
            onData = { modelsResponse ->
                updateLoading(type, false)
                _uiState.value = BaseNetWorkUiState.Success(modelsResponse)
                
                val defaultModel = modelsResponse.data.firstOrNull()?.id ?: "未选择模型"

                when (type) {
                    0 -> {
                        _models.value = modelsResponse.data.sortedBy { it.id }
                        val currentSelected = userConfig?.selectedModel
                        val position = _models.value.indexOfFirst { it.id == currentSelected }
                        onModelSelected(_models.value.getOrNull(position)?.id ?: defaultModel)
                    }
                    1 -> {
                        _imgModels.value = modelsResponse.data.sortedBy { it.id }
                        val currentSelected = userConfig?.selectedImgModel
                        val position = _imgModels.value.indexOfFirst { it.id == currentSelected }
                        onModelSelected(_imgModels.value.getOrNull(position)?.id ?: defaultModel)
                    }
                }
                ToastUtils.showToast("成功获取${modelsResponse.data.size}个模型")
            },
            onError = { message, exception ->
                Timber.tag("ApiConfigViewModel").d("Error: $message Exception: $exception")
                _uiState.value = BaseNetWorkUiState.Error(message, exception)
                updateLoading(type, false)
                ToastUtils.showToast(message)
            },
            onLoading = {
                _uiState.value = BaseNetWorkUiState.Loading
                updateLoading(type, true)
            }
        )
    }

    private fun updateLoading(type: Int, loading: Boolean) {
        if (type == 0) _apiUIState.value = _apiUIState.value.copy(isLoadingModels = loading)
        else _apiUIState.value = _apiUIState.value.copy(isLoadingImgModels = loading)
    }

    fun updateUserConfig(
        apiConfigs: List<ApiConfig>,
        currentApiConfigId: String?,
        selectedModel: String,
        imgRecognitionEnabled: Boolean,
        currentImgApiConfigId: String?,
        selectedImgModel: String
    ) {
        viewModelScope.launch {
            val newUserConfig = userConfig?.copy(
                apiConfigs = apiConfigs,
                currentApiConfigId = currentApiConfigId,
                selectedModel = selectedModel,
                imgRecognitionEnabled = imgRecognitionEnabled,
                currentImgApiConfigId = currentImgApiConfigId,
                selectedImgModel = selectedImgModel
            ) ?: return@launch
            userConfigState.updateUserConfig(newUserConfig)
            navigator.navigateBack()
        }
    }

    data class ApiConfigUIState(
        val isLoadingModels: Boolean = false,
        val isLoadingImgModels: Boolean = false,
    )
}
