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
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiHubRepository @Inject constructor(
    private val aiHubNetworkDataSource: AiHubNetworkDataSource,
    private val json: Json
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
     * 流式发送聊天消息
     */
    fun streamSendMessage(
        baseUrl: String? = null,
        apiKey: String? = null,
        request: ChatRequest
    ): Flow<String> = flow {
        val responseBody = aiHubNetworkDataSource.streamSendMessage(baseUrl, apiKey, request.copy(stream = true))
        var hasEmitted = false
        val fullResponseBuffer = StringBuilder()
        responseBody.source().use { source ->
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                fullResponseBuffer.append(line).append("\n")
                if (line.startsWith("data: ")) {
                    val data = line.substring(6)
                    if (data == "[DONE]") break
                    try {
                        val chatResponse = json.decodeFromString<ChatResponse>(data)
                        chatResponse.choices?.firstOrNull()?.delta?.content?.let {
                            emit(it)
                            hasEmitted = true
                        }
                    } catch (e: Exception) {
                        // 忽略解析失败的行
                    }
                }
            }
        }
        // --- 健壮性回退逻辑 ---
        // 如果没有通过 SSE 发送过任何数据，且缓冲区有内容，尝试按普通响应解析
        if (!hasEmitted && fullResponseBuffer.isNotEmpty()) {
            try {
                val normalResponse = json.decodeFromString<ChatResponse>(fullResponseBuffer.toString())
                normalResponse.choices?.firstOrNull()?.message?.content?.let {
                    emit(it)
                }
            } catch (e: Exception) {
                Timber.tag("AiHubRepository").e("流式回退解析失败: ${e.message}")
            }
        }
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

    /**
     * 流式发送多模态聊天消息
     */
    fun streamSendMultimodalMessage(
        baseUrl: String? = null,
        apiKey: String? = null,
        request: MultimodalChatRequest
    ): Flow<String> = flow {
        val responseBody = aiHubNetworkDataSource.streamSendMultimodalMessage(baseUrl, apiKey, request.copy(stream = true))
        responseBody.source().use { source ->
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.substring(6)
                    if (data == "[DONE]") break
                    try {
                        val chatResponse = json.decodeFromString<ChatResponse>(data)
                        chatResponse.choices?.firstOrNull()?.delta?.content?.let {
                            emit(it)
                        }
                    } catch (e: Exception) {
                        // 忽略解析失败
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}