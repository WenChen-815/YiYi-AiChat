package com.wenchen.yiyi.core.network.interceptor

import com.wenchen.yiyi.feature.config.common.ConfigManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API密钥拦截器 - 添加API密钥到请求头
 */
@Singleton
class ApiKeyInterceptor @Inject constructor(
) : Interceptor {
    /**
     * 添加API密钥到请求头
     *
     * @param chain 请求链
     * @return 响应
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = ConfigManager().getApiKey()

        val originalRequest = chain.request()
        val request = originalRequest.newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .build()
        return chain.proceed(request)
    }
}