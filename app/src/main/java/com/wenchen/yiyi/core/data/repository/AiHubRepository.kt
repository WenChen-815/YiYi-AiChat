package com.wenchen.yiyi.core.data.repository

import com.wenchen.yiyi.core.model.network.ChatResponse
import com.wenchen.yiyi.core.model.network.ModelsResponse
import com.wenchen.yiyi.core.model.network.NetworkResponse
import com.wenchen.yiyi.core.network.datasource.chat.AiHubNetworkDataSource
import com.wenchen.yiyi.core.network.service.ChatRequest
import com.wenchen.yiyi.core.network.service.MultimodalChatRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiHubRepository @Inject constructor(
    private val aiHubNetworkDataSource: AiHubNetworkDataSource
) {
    /**
     * 获取AI模型列表
     *
     * @return 包含AI模型信息的响应对象
     */
    fun getModels(baseUrl: String? =  null, apiKey: String? =  null) : Flow<NetworkResponse<ModelsResponse>> = flow {
        emit(aiHubNetworkDataSource.getModels(baseUrl, apiKey))
    }.flowOn(Dispatchers.IO)

    /**
     * 发送聊天消息
     *
     * @param baseUrl 基础URL
     * @param apiKey API密钥
     * @param request 聊天请求对象
     * @return 包含聊天响应的响应对象
     */
    fun sendMessage(baseUrl: String? =  null, apiKey: String? =  null, request: ChatRequest) : Flow<NetworkResponse<ChatResponse>> = flow {
        emit(aiHubNetworkDataSource.sendMessage(baseUrl, apiKey, request))
    }.flowOn(Dispatchers.IO)

    /**
     * 发送多模态聊天消息
     *
     * @param baseUrl 基础URL
     * @param apiKey API密钥
     * @param request 多模态聊天请求对象
     * @return 包含聊天响应的响应对象
     */
    fun sendMultimodalMessage(baseUrl: String? =  null, apiKey: String? =  null, request: MultimodalChatRequest) : Flow<NetworkResponse<ChatResponse>> = flow {
        emit(aiHubNetworkDataSource.sendMultimodalMessage(baseUrl, apiKey, request))
    }.flowOn(Dispatchers.IO)
}