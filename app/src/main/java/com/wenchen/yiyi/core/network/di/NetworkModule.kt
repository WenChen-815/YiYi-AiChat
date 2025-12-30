package com.wenchen.yiyi.core.network.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.wenchen.yiyi.BuildConfig
import com.wenchen.yiyi.core.network.interceptor.ApiKeyInterceptor
import com.wenchen.yiyi.feature.config.common.ConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Qualifier

/**
 * 网络模块
 * 负责提供网络相关的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://vg.v1api.cc"
    private val BASE_AI_URL = ConfigManager().getBaseUrl() ?: BASE_URL
    private const val IMG_AI_URL = "https://api.yiyi.ai/ai/img/"
    private const val TTS_AI_URL = "https://api.yiyi.ai/ai/tts/"



    /**
     * 提供JSON序列化配置
     *
     * @return JSON序列化实例
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /**
     * 提供OkHttpClient实例
     *
     * @param authInterceptor 认证拦截器
     * @param loggingInterceptor 日志拦截器
     * @param context 应用上下文
     * @return OkHttpClient实例
     */
    @Provides
    @Singleton
    @DefaultOkHttpClient
    fun provideOkHttpClient(
//        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        @ApplicationContext context: Context
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间
            .writeTimeout(10, TimeUnit.SECONDS) // 写超时时间
            .readTimeout(10, TimeUnit.SECONDS) // 读超时时间
//            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(ChuckerInterceptor.Builder(context).build())
                }
            }
            // 请求失败重试
            .retryOnConnectionFailure(true)
            .build()
    }
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class DefaultOkHttpClient

    /**
     * 提供Retrofit实例
     *
     * @param okHttpClient OkHttp客户端
     * @param json JSON序列化实例
     * @return Retrofit实例
     */
    @Provides
    @Singleton
    @DefaultRetrofit
    fun provideRetrofit(
        @DefaultOkHttpClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class DefaultRetrofit

    /**
     * 提供AI OkHttpClient实例
     *
     * @param apiKeyInterceptor API密钥拦截器
     * @param loggingInterceptor 日志拦截器
     * @param context 应用上下文
     * @return OkHttpClient实例
     */
    @Provides
    @Singleton
    @AiOkHttpClient
    fun provideAiOkHttpClient(
        apiKeyInterceptor: ApiKeyInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        @ApplicationContext context: Context
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间
            .writeTimeout(10, TimeUnit.SECONDS) // 写超时时间
            .readTimeout(10, TimeUnit.SECONDS) // 读超时时间
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(ChuckerInterceptor.Builder(context).build())
                }
            }
            // 请求失败重试
            .retryOnConnectionFailure(true)
            .build()
    }

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class AiOkHttpClient

    /**
     * 提供AI Retrofit实例
     *
     * @param okHttpClient OkHttp客户端
     * @param json JSON序列化实例
     * @return Retrofit实例
     */
    @Provides
    @Singleton
    @AiRetrofit
    fun provideAiRetrofit(
        @AiOkHttpClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_AI_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class AiRetrofit

    /**
     * 提供日志拦截器
     *
     * @return 日志拦截器实例
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
}
