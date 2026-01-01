package com.wenchen.yiyi.core.datastore.di

import com.wenchen.yiyi.core.datastore.datasource.config.UserConfigStoreDataSource
import com.wenchen.yiyi.core.datastore.datasource.config.UserConfigStoreDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据存储模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {
    /**
     * 绑定用户配置数据存储数据源
     */
    @Binds
    @Singleton
    abstract fun bindUserConfigStoreDataSource(
        impl : UserConfigStoreDataSourceImpl
    ): UserConfigStoreDataSource
}