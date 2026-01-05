package com.wenchen.yiyi.feature.main.viewmodel


import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.core.base.state.BaseNetWorkUiState
import com.wenchen.yiyi.core.base.viewmodel.BaseNetWorkViewModel
import com.wenchen.yiyi.core.model.network.Model
import com.wenchen.yiyi.core.model.network.ModelsResponse
import com.wenchen.yiyi.core.data.repository.AiHubRepository
import com.wenchen.yiyi.core.model.network.NetworkResponse
import com.wenchen.yiyi.core.result.ResultHandler
import com.wenchen.yiyi.core.result.asResult
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * 主界面 ViewModel
 *
 * @param navigator 导航控制器
 * @param userState 全局用户状态
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    private val aiHubRepository: AiHubRepository
) : BaseNetWorkViewModel<ModelsResponse>(
    navigator = navigator,
    userState = userState
) {
    private val _models = MutableStateFlow<List<Model>>(emptyList())
    val models: StateFlow<List<Model>> = _models.asStateFlow()
    fun executeGetModels() {
        Timber.tag("test").d("Executing getModels")
        ResultHandler.handleResultWithData(
            scope = viewModelScope,
            flow = requestApiFlow().asResult(),
            onData = { modelsResponse ->
                Timber.tag("test").d("Received models: $modelsResponse")
                // 更新 models 状态
                _models.value = modelsResponse.data
                // 更新 uiState 为成功状态
                _uiState.value = BaseNetWorkUiState.Success(modelsResponse)
            },
            onError = { message, exception ->
                // 更新 uiState 为错误状态
                _uiState.value = BaseNetWorkUiState.Error(message, exception)
            },
            onLoading = {
                // 更新 uiState 为加载状态
                _uiState.value = BaseNetWorkUiState.Loading
            }
        )
    }

    override fun requestApiFlow(): Flow<NetworkResponse<ModelsResponse>> {
        return aiHubRepository.getModels()
    }
}
