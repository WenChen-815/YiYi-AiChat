package com.wenchen.yiyi.core.network.interceptor

import com.wenchen.yiyi.core.state.UserConfigState
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API密钥和动态BaseUrl拦截器
 * - 优先从请求头中获取临时的baseUrl和apiKey。
 * - 如果请求头中没有，则回退到从UserConfigState中获取。
 * - 将最终的API密钥添加到请求的Authorization头中。
 * - 根据最终的baseUrl动态修改请求的URL。
 */
@Singleton
class ApiKeyInterceptor @Inject constructor(
    private val userConfigState: UserConfigState
) : Interceptor {

    companion object {
        const val CUSTOM_BASE_URL_HEADER = "X-Custom-Base-Url"
        const val CUSTOM_API_KEY_HEADER = "X-Custom-Api-Key"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // 从请求头中获取自定义的baseUrl和apiKey
        val customBaseUrl = originalRequest.header(CUSTOM_BASE_URL_HEADER)
        val customApiKey = originalRequest.header(CUSTOM_API_KEY_HEADER)

        // 从请求构建器中移除这些自定义头，避免将其发送到服务器
        requestBuilder.removeHeader(CUSTOM_BASE_URL_HEADER)
        requestBuilder.removeHeader(CUSTOM_API_KEY_HEADER)

        // 优先使用自定义值，否则回退到UserConfigState中的配置
        val userConfig = userConfigState.userConfig.value
        val apiKey = (customApiKey ?: userConfig?.baseApiKey)?.trim()
        val baseUrl = (customBaseUrl ?: userConfig?.baseUrl)?.trim()

        // 添加API密钥
        apiKey?.takeIf { it.isNotEmpty() }?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }

        // 动态修改baseUrl
        if (!baseUrl.isNullOrEmpty()) {
            baseUrl.toHttpUrlOrNull()?.let { newHttpUrl ->
                val finalUrl = buildFinalUrl(newHttpUrl, originalRequest.url)
                requestBuilder.url(finalUrl)
            } ?: Timber.tag("ApiKeyInterceptor").w("Invalid baseUrl: $baseUrl")
        }

        // 构建最终的请求
        val finalRequest = requestBuilder.build()

        // 输出请求头信息
        printRequestHeaders(finalRequest)

        return chain.proceed(finalRequest)
    }

    private fun buildFinalUrl(newHttpUrl: HttpUrl, originalUrl: HttpUrl): HttpUrl {
        // 从用户自定义的 baseUrl 获取路径, 并处理 "/v1" 的特殊情况
        val pathPrefix = newHttpUrl.encodedPath.let {
            if (it == "/v1" || it == "/v1/") "" else it.removeSuffix("/")
        }

        // 从 Retrofit 接口获取原始请求路径
        val servicePath = originalUrl.encodedPath

        // 组合路径，并构建新的 URL，同时保留原始的查询参数和片段
        return newHttpUrl.newBuilder()
            .encodedPath(pathPrefix + servicePath)
            .encodedQuery(originalUrl.encodedQuery)
            .encodedFragment(originalUrl.encodedFragment)
            .build()
    }

    /**
     * 打印请求头信息
     *
     * @param request OkHttp请求对象
     */
    private fun printRequestHeaders(request: Request) {
        val headersInfo = buildString {
            append("===== Request Headers =====\n")
            append("- Method: ${request.method}\n")
            append("- URL: ${request.url}\n")
            for (i in 0 until request.headers.size) {
                val name = request.headers.name(i)
                val value = request.headers.value(i)
                append("- Header: $name = $value\n")
            }

            // 获取并打印请求体
            val body = request.body
            if (body != null) {
                // 尝试获取请求体内容
                val buffer = okio.Buffer()
                try {
                    body.writeTo(buffer)
                    val bodyContent = buffer.readUtf8()
                    // append("- Body: $bodyContent\n")
                    val regex = Regex("\"messages\":\\[[\\s\\S]*?\\],\"")
                    val redactedBody = bodyContent.replace(regex, "\"message\":[MESSAGE],\"")
                    append("- Body: $redactedBody\n")
                } catch (e: Exception) {
                    append("- Body: ${body.contentType()} (无法读取内容 - ${e.message})\n")
                }
            } else {
                append("- Body: null\n")
            }

            append("=========================")
        }
        Timber.tag("ApiKeyInterceptor").d(headersInfo.trim())
    }
}