package com.wenchen.yiyi.core.datastore.datasource.config

import com.wenchen.yiyi.core.model.config.UserConfig

interface UserConfigStoreDataSource {
    companion object {
        const val USER_CONFIG_KEY = "user_config"
    }

    suspend fun saveUserConfig(userConfig: UserConfig)

    suspend fun getUserConfig(): UserConfig?

    suspend fun clearUserConfig()
}