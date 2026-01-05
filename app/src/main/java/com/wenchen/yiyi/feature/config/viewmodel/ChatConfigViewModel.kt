package com.wenchen.yiyi.feature.config.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.core.base.state.BaseNetWorkUiState
import com.wenchen.yiyi.core.base.viewmodel.BaseNetWorkViewModel
import com.wenchen.yiyi.core.common.ApiService
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    fun loadModels(
        apiKey: String,
        baseUrl: String,
        type: Int,
        coroutineScope: CoroutineScope,
        setLoading: (Boolean) -> Unit,
        setModels: (List<Model>) -> Unit,
        setSelectedModel: (String) -> Unit,
        showSuccessToast: ((String) -> Unit)? = null,
        showErrorToast: ((String) -> Unit)? = null
    ) {
        setLoading(true)

        coroutineScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e("ConfigActivity", "IO协程异常", throwable)
            setLoading(false)
        }) {
            val apiService = ApiService(baseUrl, apiKey)
            apiService.getSupportedModels(
                onSuccess = { modelList ->
                    Log.d("ConfigActivity", "获取模型列表成功, 模型数量: ${modelList.size}")
                    try {
                        // 直接在当前线程更新数据，然后启动Main线程更新UI
                        setModels(modelList)

                        // 设置默认选中项
                        var selectedId: String
                        if (type == 0) {
//                            val savedModel = configManager.getSelectedModel()
                            val savedModel = userConfig?.selectedModel
                            if (savedModel != null) {
                                val position = modelList.indexOfFirst { it.id == savedModel }
                                selectedId = if (position != -1) {
                                    Log.d("ConfigActivity", "找到保存的模型: ${modelList[position].id}")
                                    modelList[position].id
                                } else {
                                    // 未找到保存的模型，选中第一个
                                    Log.d(
                                        "ConfigActivity",
                                        "未找到保存的模型，选中第一个: ${modelList.firstOrNull()?.id}"
                                    )
                                    modelList.firstOrNull()?.id ?: ""
                                }
                            } else {
                                // 没有保存的模型，选中第一个
                                Log.d(
                                    "ConfigActivity",
                                    "没有保存的模型，选中第一个: ${modelList.firstOrNull()?.id}"
                                )
                                selectedId = modelList.firstOrNull()?.id ?: ""
                            }
                        } else {
                            val savedImgModel = userConfig?.selectedImgModel
                            if (savedImgModel != null) {
                                val position = modelList.indexOfFirst { it.id == savedImgModel }
                                selectedId = if (position != -1) {
                                    Log.d(
                                        "ConfigActivity",
                                        "找到保存的图片识别模型: ${modelList[position].id}"
                                    )
                                    modelList[position].id
                                } else {
                                    // 未找到保存的模型，选中第一个
                                    Log.d(
                                        "ConfigActivity",
                                        "未找到保存的图片识别模型，选中第一个: ${modelList.firstOrNull()?.id}"
                                    )
                                    modelList.firstOrNull()?.id ?: ""
                                }
                            } else {
                                // 没有保存的模型，选中第一个
                                Log.d(
                                    "ConfigActivity",
                                    "没有保存的图片识别模型，选中第一个: ${modelList.firstOrNull()?.id}"
                                )
                                selectedId = modelList.firstOrNull()?.id ?: ""
                            }
                        }


                        // 在主线程中执行UI更新操作
                        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                            try {
                                Log.d("ConfigActivity", "设置默认选中项: $selectedId")
                                if (selectedId.isNotEmpty()) {
                                    setSelectedModel(selectedId)
                                }
                                setLoading(false)
                                showSuccessToast?.invoke("模型加载成功，共${modelList.size}个模型")
                            } catch (e: Exception) {
                                Log.e("ConfigActivity", "设置模型列表或选中项时出错", e)
                                setLoading(false)
                                showErrorToast?.invoke("模型加载失败: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ConfigActivity", "处理模型数据时出错", e)
                        // 确保无论如何都停止加载状态
                        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                            setLoading(false)
                            showErrorToast?.invoke("模型加载失败: ${e.message}")
                        }
                    }
                },
                onError = { errorMsg ->
                    Log.e("ConfigActivity", "加载模型失败: $errorMsg")
                    // 确保在主线程中更新UI
                    CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                        setLoading(false)
                        showErrorToast?.invoke("模型加载失败: $errorMsg")
                    }
                }
            )
        }
    }
}