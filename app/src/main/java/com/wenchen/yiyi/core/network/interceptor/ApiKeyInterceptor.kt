package com.wenchen.yiyi.core.network.interceptor

import com.wenchen.yiyi.core.state.UserConfigState
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
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
        val apiKey = customApiKey ?: userConfigState.userConfig.value?.baseApiKey
        val baseUrl = customBaseUrl ?: userConfigState.userConfig.value?.baseUrl

        // 添加API密钥
        if (!apiKey.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        // 动态修改baseUrl
        if (!baseUrl.isNullOrEmpty()) {
            val newHttpUrl = baseUrl.toHttpUrlOrNull()
            if (newHttpUrl != null) {
                val newUrl = originalRequest.url.newBuilder()
                    .scheme(newHttpUrl.scheme)
                    .host(newHttpUrl.host)
                    .port(newHttpUrl.port)
                    .build()
                requestBuilder.url(newUrl)
                Timber.tag("ApiKeyInterceptor").d("Redirected URL: $newUrl")
            } else {
                Timber.tag("ApiKeyInterceptor").w("Invalid baseUrl: $baseUrl")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
