package com.wenchen.yiyi.core.network.datasource.chat

import com.wenchen.yiyi.core.model.network.ChatResponse
import com.wenchen.yiyi.core.model.network.ModelsResponse
import com.wenchen.yiyi.core.model.network.NetworkResponse
import com.wenchen.yiyi.core.network.service.ChatRequest
import com.wenchen.yiyi.core.network.service.MultimodalChatRequest
import okhttp3.ResponseBody

/**
 * AI模型网络数据源接口
 * 提供AI模型相关的网络数据操作
 */
interface AiHubNetworkDataSource {
    /**
     * 获取AI模型列表
     *
     * @return 模型列表响应
     */
    suspend fun getModels(baseUrl: String? =  null, apiKey: String? =  null): NetworkResponse<ModelsResponse>

    suspend fun sendMessage(
        baseUrl: String? =  null,
        apiKey: String? =  null,
        request: ChatRequest
    ): NetworkResponse<ChatResponse>

    suspend fun streamSendMessage(
        baseUrl: String? = null,
        apiKey: String? = null,
        request: ChatRequest
    ): ResponseBody

    suspend fun sendMultimodalMessage(
        baseUrl: String? =  null,
        apiKey: String? =  null,
        request: MultimodalChatRequest
    ): NetworkResponse<ChatResponse>

    suspend fun streamSendMultimodalMessage(
        baseUrl: String? = null,
        apiKey: String? = null,
        request: MultimodalChatRequest
    ): ResponseBody
}