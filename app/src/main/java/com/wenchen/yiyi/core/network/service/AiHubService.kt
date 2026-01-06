package com.wenchen.yiyi.core.network.service

import com.wenchen.yiyi.core.model.network.ChatResponse
import com.wenchen.yiyi.core.model.network.Message
import com.wenchen.yiyi.core.model.network.ModelsResponse
import com.wenchen.yiyi.core.network.interceptor.ApiKeyInterceptor
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * AI服务接口
 * 提供AI模型、聊天等相关的网络数据操作
 */
interface AiHubService {

    /**
     * 获取AI模型列表
     *
     * @param baseUrl 动态指定的BaseUrl，会覆盖默认值
     * @param apiKey 动态指定的ApiKey，会覆盖默认值
     * @return 模型列表响应
     */
    @GET("v1/models")
    suspend fun getModels(
        @Header(ApiKeyInterceptor.CUSTOM_BASE_URL_HEADER) baseUrl: String? = null,
        @Header(ApiKeyInterceptor.CUSTOM_API_KEY_HEADER) apiKey: String? = null
    ): ModelsResponse

    /**
     * 发送聊天消息到AI模型API。
     * @param baseUrl 动态指定的BaseUrl，会覆盖默认值
     * @param apiKey 动态指定的ApiKey，会覆盖默认值
     * @param request 聊天请求体
     */
    @POST("v1/chat/completions")
    suspend fun sendMessage(
        @Header(ApiKeyInterceptor.CUSTOM_BASE_URL_HEADER) baseUrl: String?,
        @Header(ApiKeyInterceptor.CUSTOM_API_KEY_HEADER) apiKey: String?,
        @Body request: ChatRequest
    ): ChatResponse

    /**
     * 发送多模态消息（文本+图像）到AI模型。
     * @param baseUrl 动态指定的BaseUrl，会覆盖默认值
     * @param apiKey 动态指定的ApiKey，会覆盖默认值
     * @param request 多模态聊天请求体
     */
    @POST("v1/chat/completions")
    suspend fun sendMultimodalMessage(
        @Header(ApiKeyInterceptor.CUSTOM_BASE_URL_HEADER) baseUrl: String?,
        @Header(ApiKeyInterceptor.CUSTOM_API_KEY_HEADER) apiKey: String?,
        @Body request: MultimodalChatRequest
    ): ChatResponse
}

// --- Data Classes for Requests ---

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float? = null
)

@Serializable
data class MultimodalChatRequest(
    val model: String,
    val messages: List<MultimodalMessage>,
    val temperature: Float? = null
)

@Serializable
data class MultimodalMessage(
    val role: String,
    val content: List<ContentItem>
)

@Serializable
data class ContentItem(
    val type: String,
    val text: String? = null,
    val image_url: Map<String, String>? = null
)
