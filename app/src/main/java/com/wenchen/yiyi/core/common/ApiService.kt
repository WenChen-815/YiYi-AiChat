package com.wenchen.yiyi.core.common

import android.util.Log
import com.google.gson.Gson
import com.wenchen.yiyi.core.database.entity.ChatResponse
import com.wenchen.yiyi.core.database.entity.Message
import com.wenchen.yiyi.core.database.entity.Model
import com.wenchen.yiyi.core.database.entity.ModelsResponse
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiService(
    private val baseUrl: String,
    private val apiKey: String,
    private val imgBaseUrl: String? = null,
    private val imgApiKey: String? = null,
    private val ttsBaseUrl: String? = null,
    private val ttsApiKey: String? = null
) {
    private val tag = "ApiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

    /**
     * 发送聊天消息到AI模型API，并处理返回结果
     *
     * @param messages 消息列表，包含系统提示、历史对话和最新用户输入
     * @param model 使用的AI模型名称
     * @param temperature 控制生成文本随机性的参数，默认值为1.0
     * @param onSuccess 成功回调函数，接收AI生成的回复文本
     * @param onError 失败回调函数，接收错误信息
     */
    fun sendMessage(
        messages: MutableList<Message>,
        model: String,
        temperature: Float? = 1.00f,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
//        Log.i(tag, "model: $model")
        // 创建请求体对象
        val chatRequest = ChatRequest(model, messages, temperature)

        // 使用Gson将请求体对象转换为JSON字符串
        val requestBodyStr = gson.toJson(chatRequest)
        val requestBody = requestBodyStr.toRequestBody(jsonMediaType)
//        Log.d(tag, requestBodyStr)
        // 构建请求
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("网络请求失败：${e.message ?: "未知错误"}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body.string()
                if (response.isSuccessful) {
                    try {
                        val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                        val aiMessage = chatResponse.choices?.firstOrNull()?.message?.content
                        if (aiMessage.isNullOrEmpty()) {
                                Log.d(tag, "未获取到AI回复 responseBody：$responseBody")
                                onError("未获取到AI回复")
                        } else {
                            // 去除回复内容中的换行符
                            // val cleanedMessage = aiMessage.replace("\n", "")
                            // onSuccess(cleanedMessage)
                            onSuccess(aiMessage)
                        }
                    } catch (e: Exception) {
                        onError("解析响应失败：${e.message}")
                    }
                } else {
                    onError("请求失败：$responseBody（状态码：${response.code}）")
                    Log.d(tag, "请求失败：$responseBody（状态码：${response.code}）")
                }
            }
        })
    }

    /**
     * 获取支持的AI模型列表。
     *
     * @param onSuccess 当成功获取模型列表时调用，参数为模型列表。
     * @param onError 当发生错误时调用，参数为错误信息。
     */
    fun getSupportedModels(
        onSuccess: (List<Model>) -> Unit,
        onError: (String) -> Unit
    ) {
        // 构建请求
        val request = Request.Builder()
            .url("$baseUrl/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("网络请求失败：${e.message ?: "未知错误"}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body.string()
                Log.e(tag, responseBody)
                if (response.isSuccessful) {
                    try {
                        val modelsResponse = gson.fromJson(responseBody, ModelsResponse::class.java)
                        if (modelsResponse.success || modelsResponse.`object` == "list") {
                            onSuccess(modelsResponse.data)
                        } else {
                            onError("获取模型列表失败：接口返回不成功")
                        }
                    } catch (e: Exception) {
                        onError("解析模型列表失败：${e.message}")
                    }
                } else {
                    onError("获取模型列表失败：$responseBody（状态码：${response.code}）")
                }
            }
        })
    }

    /**
     * 发送多模态消息（文本+图像）到AI模型，并异步获取AI回复结果。
     *
     * @param prompt 用户输入的提示文本，不能为空。
     * @param imageBase64 图像的Base64编码字符串，不能为空。
     * @param model 要使用的AI模型名称。
     * @param temperature 控制生成结果的随机性，可选，默认值为0.80，越小描述越准确。
     * @param onSuccess 当成功获取AI回复时调用，参数为AI返回的内容。
     * @param onError 当发生错误时调用，参数为错误信息。
     */
    fun sendMultimodalMessage(
        prompt: String,
        imageBase64: String,
        model: String,
        temperature: Float? = 0.80f,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // 参数校验
        if (prompt.isBlank()) {
            onError("提示文本不能为空")
            return
        }
        if (imageBase64.isBlank()) {
            onError("图片Base64内容不能为空")
            return
        }
        // 创建请求体对象
        val contentItems = mutableListOf<ContentItem>().apply {
            add(ContentItem("text", prompt))
            add(
                ContentItem(
                    "image_url",
                    image_url = mapOf("url" to "data:image/jpeg;base64,$imageBase64")
                )
            )
        }

        val message = MultimodalMessage("user", contentItems)
        val messages = listOf(message)

        val chatRequest = MultimodalChatRequest(model, messages, temperature)

        val requestBodyStr = gson.toJson(chatRequest)
        val requestBody = requestBodyStr.toRequestBody(jsonMediaType)

        // 构建请求
        val request = Request.Builder()
            .url("$imgBaseUrl/v1/chat/completions")
            .addHeader("Authorization", "Bearer $imgApiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("网络请求失败：${e.message ?: "未知错误"}")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                            val aiMessage = chatResponse.choices?.firstOrNull()?.message?.content
                            if (aiMessage.isNullOrEmpty()) {
                                Log.d(tag, "未获取到AI回复 responseBody：$responseBody")
                                onError("未获取到AI回复")
                            } else {
                                onSuccess(aiMessage)
                            }
                        } catch (e: Exception) {
                            onError("解析响应失败：${e.message}")
                        }
                    } else {
                        onError("请求失败：$responseBody（状态码：${response.code}）")
                        Log.d(
                            tag,
                            "Multimodal request failed: $responseBody (code: ${response.code})"
                        )
                    }
                } catch (e: IOException) {
                    onError("读取响应失败：${e.message}")
                }
            }
        })
    }

    // 定义聊天请求体的数据类
    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Float? = null
    )

    // 定义多模态聊天请求体的数据类
    data class MultimodalChatRequest(
        val model: String,
        val messages: List<MultimodalMessage>,
        val temperature: Float? = null
    )

    data class MultimodalMessage(
        val role: String,
        val content: List<ContentItem>
    )

    data class ContentItem(
        val type: String,
        val text: String? = null,
        val image_url: Map<String, String>? = null
    )
}