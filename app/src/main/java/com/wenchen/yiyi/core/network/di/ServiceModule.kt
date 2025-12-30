package com.wenchen.yiyi.core.network.di

import com.wenchen.yiyi.core.network.service.AiHubService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    /**
     * 提供AI中转服务实例
     *
     * @param retrofit Retrofit实例
     * @return AI中转服务实例
     */
    @Provides
    @Singleton
    fun provideAiHubService(@NetworkModule.AiRetrofit retrofit: Retrofit): AiHubService {
        return retrofit.create(AiHubService::class.java)
    }
}