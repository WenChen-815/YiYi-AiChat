package com.wenchen.yiyi.core.network.datasource.chat

import com.wenchen.yiyi.core.model.network.ChatResponse
import com.wenchen.yiyi.core.model.network.ModelsResponse
import com.wenchen.yiyi.core.model.network.NetworkResponse
import com.wenchen.yiyi.core.network.base.BaseNetworkDataSource
import com.wenchen.yiyi.core.network.service.AiHubService
import com.wenchen.yiyi.core.network.service.ChatRequest
import com.wenchen.yiyi.core.network.service.MultimodalChatRequest
import javax.inject.Inject

/**
 * AI模型网络数据源实现类
 * 提供AI模型相关的网络数据操作
 */
class AiHubNetworkDataSourceImpl @Inject constructor(
    private val aiModelsApiService: AiHubService
) : AiHubNetworkDataSource, BaseNetworkDataSource() {

    override suspend fun getModels(baseUrl: String?, apiKey: String?): NetworkResponse<ModelsResponse> {
        val response = aiModelsApiService.getModels(baseUrl, apiKey)
        return NetworkResponse(
            data = response,
            code = 1000,
            message = null
        )
    }

    override suspend fun sendMessage(
        baseUrl: String?,
        apiKey: String?,
        request: ChatRequest
    ): NetworkResponse<ChatResponse> {
        val response = aiModelsApiService.sendMessage(baseUrl, apiKey, request)
        return NetworkResponse(
            data = response,
            code = 1000,
            message = null
        )
    }

    override suspend fun sendMultimodalMessage(
        baseUrl: String?,
        apiKey: String?,
        request: MultimodalChatRequest
    ): NetworkResponse<ChatResponse> {
        val response = aiModelsApiService.sendMultimodalMessage(baseUrl, apiKey, request)
        return NetworkResponse(
            data = response,
            code = 1000,
            message = null
        )
    }
}