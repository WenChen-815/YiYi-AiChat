package com.wenchen.yiyi.core.data.repository

import com.wenchen.yiyi.core.datastore.datasource.config.UserConfigStoreDataSource
import com.wenchen.yiyi.core.model.config.UserConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserConfigStoreRepository @Inject constructor(
    private val userConfigStoreDataSource: UserConfigStoreDataSource
) {
    suspend fun saveUserConfig(userConfig: UserConfig) {
        userConfigStoreDataSource.saveUserConfig(userConfig)
    }

    suspend fun getUserConfig(): UserConfig? {
        return userConfigStoreDataSource.getUserConfig()
    }

    suspend fun clearUserConfig() {
        userConfigStoreDataSource.clearUserConfig()
    }
}