package com.wenchen.yiyi.core.network.di

import com.wenchen.yiyi.core.network.datasource.chat.AiHubNetworkDataSource
import com.wenchen.yiyi.core.network.datasource.chat.AiHubNetworkDataSourceImpl
import com.wenchen.yiyi.core.network.service.AiHubService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据源模块，提供所有网络数据源的依赖注入
 * 为Hilt提供各种网络数据源的实例
 */
@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    /**
     * 提供AI模型网络数据源实例
     *
     * @param aiHubService AI模型服务接口实例，用于网络请求
     * @return AI模型网络数据源实例
     */
    @Provides
    @Singleton
    fun provideAiHubNetworkDataSource(aiHubService: AiHubService): AiHubNetworkDataSource {
        return AiHubNetworkDataSourceImpl(aiHubService)
    }
}